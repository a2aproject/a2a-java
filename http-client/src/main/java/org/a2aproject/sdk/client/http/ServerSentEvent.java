package org.a2aproject.sdk.client.http;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a parsed Server-Sent Event (SSE).
 * <p>
 * Each instance carries the fields defined by the SSE specification:
 * <ul>
 * <li>{@code data} — the event payload, already concatenated from one or more {@code data:} lines</li>
 * <li>{@code eventType} — the event type from the {@code event:} field; never null, defaults to {@code "message"}</li>
 * <li>{@code id} — the event ID from the {@code id:} field; null if absent</li>
 * <li>{@code retry} — the reconnection interval in milliseconds from the {@code retry:} field; null if absent</li>
 * </ul>
 */
public record ServerSentEvent(String data, String eventType, @Nullable String id, @Nullable Long retry) {

    /**
     * Default event type per the SSE specification when no {@code event:} field is present.
     */
    public static final String DEFAULT_EVENT_TYPE = "message";

    public ServerSentEvent {
        Assert.checkNotNullParam("data", data);
        Assert.checkNotNullParam("eventType", eventType);
    }

    public ServerSentEvent(String data) {
        this(data, DEFAULT_EVENT_TYPE, null, null);
    }
}
