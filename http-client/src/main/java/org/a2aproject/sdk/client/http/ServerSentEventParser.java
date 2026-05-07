package org.a2aproject.sdk.client.http;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Streaming parser for Server-Sent Events (SSE).
 * <p>
 * Feed lines one at a time via {@link #processLine}; call {@link #flush} when the stream ends.
 * Not thread-safe — each connection should use its own instance.
 */
public class ServerSentEventParser {
    private static final Logger log = Logger.getLogger(ServerSentEventParser.class.getName());

    private static final int MAX_BUFFER_SIZE = 1000;
    private static final int MAX_BUFFER_CHARS = 1024 * 1024; // 1 MB (Java chars, so up to 2 MB in UTF-16; actual UTF-8 bytes may differ)
    private static final int MAX_LINE_LENGTH = 65536;         // 64 KB

    private final Consumer<ServerSentEvent> eventConsumer;
    private final @Nullable Consumer<Throwable> errorConsumer;
    private final List<String> dataBuffer = new ArrayList<>();
    private int dataBufferChars = 0;
    private @Nullable String eventType;
    // currentEventId: the spec's "event ID buffer" — persists across events, overwritten by each "id:" field.
    // lastEventId: the spec's "last event ID string" — copied from currentEventId only at dispatch (empty line),
    //   so a skipped/corrupt event block cannot advance it. Returned to callers as the reconnect Last-Event-ID.
    private @Nullable String currentEventId;
    private @Nullable String lastEventId;
    private @Nullable Long retry;
    // Set when the current event block is corrupt (line too long, buffer overflow).
    // All further fields are ignored until the next empty-line boundary.
    private boolean skippingCurrentEvent = false;

    public ServerSentEventParser(Consumer<ServerSentEvent> eventConsumer) {
        this(eventConsumer, null);
    }

    public ServerSentEventParser(Consumer<ServerSentEvent> eventConsumer, @Nullable Consumer<Throwable> errorConsumer) {
        this.eventConsumer = eventConsumer;
        this.errorConsumer = errorConsumer;
    }

    /**
     * Processes a single line from the SSE stream. An empty line dispatches any buffered event.
     * A {@code null} line is routed to the error consumer (or logged) without affecting the current event block.
     */
    public void processLine(@Nullable String line) {
        if (line == null) {
            handleError(new IllegalArgumentException("Line cannot be null"));
            return;
        }

        // Check line length to prevent DoS; corrupt the current event so it is not dispatched
        if (line.length() > MAX_LINE_LENGTH) {
            handleError(new IllegalArgumentException("Line exceeds maximum length of " + MAX_LINE_LENGTH + " characters"));
            skippingCurrentEvent = true;
            dataBuffer.clear();
            dataBufferChars = 0;
            return;
        }

        if (skippingCurrentEvent && !line.isEmpty()) {
            return;
        }

        // Empty line - dispatch the buffered event
        if (line.isEmpty()) {
            dispatchEvent();
            return;
        }

        // Comment line - ignore
        if (line.startsWith(":")) {
            return;
        }

        // Parse field and value
        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) {
            // Field with no value
            processField(line, "");
        } else {
            String field = line.substring(0, colonIndex);
            String value = line.substring(colonIndex + 1);

            // Remove optional leading space from value
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }

            processField(field, value);
        }
    }

    private void processField(String field, String value) {
        switch (field) {
            case "data" -> {
                // Check line count to prevent DoS; corrupt and skip the rest of this event block
                if (dataBuffer.size() >= MAX_BUFFER_SIZE) {
                    handleError(new IllegalStateException("SSE data buffer exceeded maximum size of " + MAX_BUFFER_SIZE + " lines"));
                    skippingCurrentEvent = true;
                    dataBuffer.clear();
                    dataBufferChars = 0;
                    return;
                }
                // Check total char count to prevent OOM on large streams
                if (dataBufferChars + value.length() > MAX_BUFFER_CHARS) {
                    handleError(new IllegalStateException("SSE data buffer exceeded maximum size of " + MAX_BUFFER_CHARS + " chars"));
                    skippingCurrentEvent = true;
                    dataBuffer.clear();
                    dataBufferChars = 0;
                    return;
                }
                dataBuffer.add(value);
                dataBufferChars += value.length();
            }
            case "event" -> eventType = value;
            case "id" -> {
                // Per SSE spec: ignore the id field if the value contains a U+0000 NULL character.
                // An empty value is valid and clears the last event ID buffer on dispatch.
                if (value.indexOf('\0') == -1) {
                    currentEventId = value;
                }
            }
            case "retry" -> {
                // Per SSE spec: ignore the retry field unless the value consists entirely of ASCII digits.
                if (!value.isEmpty() && value.chars().allMatch(c -> c >= '0' && c <= '9')) {
                    try {
                        retry = Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        // Value is all digits but too large for long; log and ignore per spec.
                        log.fine("Ignoring retry value out of long range: " + value);
                    }
                } else {
                    log.fine("Ignoring non-digit retry value: " + value);
                }
            }
            default -> {
                // Unknown field - ignore per spec
                log.fine("Ignoring unknown SSE field: " + field);
            }
        }
    }

    private void dispatchEvent() {
        // Per SSE spec: update lastEventId before checking data, so ID-only events (e.g. heartbeats) are tracked
        if (currentEventId != null) {
            lastEventId = currentEventId;
        }

        String data = String.join("\n", dataBuffer);
        String type = eventType;
        String id = currentEventId;

        // Always reset at event boundary, regardless of whether an event is dispatched
        dataBuffer.clear();
        dataBufferChars = 0;
        eventType = null;
        skippingCurrentEvent = false;
        // currentEventId is NOT reset — it persists per the SSE specification

        // Per SSE spec: don't dispatch if data is empty (also covers limit-violation blocks, whose buffer was cleared)
        if (data.isEmpty()) {
            return;
        }

        eventConsumer.accept(new ServerSentEvent(data, type != null ? type : ServerSentEvent.DEFAULT_EVENT_TYPE, id, retry));
    }

    /** Dispatches any buffered data not yet followed by an empty line. Call when the stream ends. */
    public void flush() {
        dispatchEvent();
    }

    /** Returns the last event ID received, or {@code null} if none. */
    public @Nullable String getLastEventId() {
        return lastEventId;
    }

    /** Returns the reconnection interval in milliseconds from the last {@code retry:} field, or {@code null} if none. */
    public @Nullable Long getRetry() {
        return retry;
    }

    private void handleError(Throwable error) {
        if (errorConsumer != null) {
            errorConsumer.accept(error);
        } else {
            log.warning("SSE parsing error: " + error.getMessage());
        }
    }
}
