package io.a2a.client.http.vertx.sse;

import io.a2a.client.http.sse.CommentEvent;
import io.a2a.client.http.sse.DataEvent;
import io.a2a.client.http.sse.Event;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

public class SSEHandler implements Handler<Buffer> {


    private static final Logger LOG = LoggerFactory.getLogger(SSEHandler.class);

    private static final String UTF8_BOM = "\uFEFF";

    private static final String DEFAULT_EVENT_NAME = "message";

    private String currentEventName = DEFAULT_EVENT_NAME;
    private final StringBuilder dataBuffer = new StringBuilder();

    private String lastEventId = "";

    private final Consumer<Event> eventConsumer;

    public SSEHandler(Consumer<Event> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    private void handleFieldValue(String fieldName, String value) {
        switch (fieldName) {
            case "event":
                currentEventName = value;
                break;
            case "data":
                dataBuffer.append(value).append("\n");
                break;
            case "id":
                if (!value.contains("\0")) {
                    lastEventId = value;
                }
                break;
            case "retry":
                // ignored
                break;
        }
    }

    private String stripLeadingSpaceIfPresent(String field) {
        if (field.charAt(0) == ' ') {
            return field.substring(1);
        }
        return field;
    }

    private String removeLeadingBom(String input) {
        if (input.startsWith(UTF8_BOM)) {
            return input.substring(UTF8_BOM.length());
        }
        return input;
    }

    private String removeTrailingNewline(String input) {
        if (input.endsWith("\n")) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }

    private Buffer buffer = Buffer.buffer();

    @Override
    public void handle(Buffer chunk) {
        buffer.appendBuffer(chunk);
        int separatorIndex;
        // The separator for events is a double newline
        String separator = "\n\n";
        while ((separatorIndex = buffer.toString().indexOf(separator)) != -1) {
            Buffer eventData = buffer.getBuffer(0, separatorIndex);
            parse(eventData.toString());
            buffer = buffer.getBuffer(separatorIndex + separator.length(), buffer.length());
        }
    }

    private void parse(String input) {
        String[] parts = input.split("\n");

        for (String part : parts) {
            LOG.debug("got line `{}`", part);
            String line = removeTrailingNewline(removeLeadingBom(part));

            if (line.startsWith(":")) {
                eventConsumer.accept(new CommentEvent(line.substring(1).trim()));
            } else if (line.contains(":")) {
                List<String> lineParts = List.of(line.split(":", 2));
                if (lineParts.size() == 2) {
                    handleFieldValue(lineParts.get(0), stripLeadingSpaceIfPresent(lineParts.get(1)));
                }
            } else {
                handleFieldValue(line, "");
            }
        }

        LOG.debug(
                "broadcasting new event named {} lastEventId is {}",
                currentEventName,
                lastEventId
        );

        if (!dataBuffer.isEmpty()) {
            // Remove trailing newline
            dataBuffer.setLength(dataBuffer.length() - 1);
            eventConsumer.accept(new DataEvent(currentEventName, dataBuffer.toString(), lastEventId));
        }

        // reset
        dataBuffer.setLength(0);
        currentEventName = DEFAULT_EVENT_NAME;
    }
}
