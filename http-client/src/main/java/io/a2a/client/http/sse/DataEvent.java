package io.a2a.client.http.sse;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Represents an SSE DataEvent
 * It contains three fields: event name, data, and lastEventId
 */
public class DataEvent extends Event {

    private final String eventName;
    private final String data;
    private final String lastEventId;

    public DataEvent(String eventName, String data, String lastEventId) {
        this.eventName = eventName;
        this.data = data;
        this.lastEventId = lastEventId;
    }

    @Override
    Type getType() {
        return Type.DATA;
    }

    /**
     *
     * @return the content of the last line starting with `event:`
     */
    public String getEventName() {
        return eventName;
    }

    /**
     *
     * @return the accumulated contents of data buffers from lines starting with `data:`
     */
    public String getData() {
        return data;
    }

    /**
     *
     * @return the last event id sent in a line starting with `id:`
     */
    public String getLastEventId() {
        return lastEventId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DataEvent.class.getSimpleName() + "[", "]")
                .add("eventName='" + eventName + "'")
                .add("data='" + data + "'")
                .add("lastEventId='" + lastEventId + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataEvent event = (DataEvent) o;
        return (
                Objects.equals(getType(), event.getType()) &&
                        Objects.equals(eventName, event.eventName) &&
                        Objects.equals(data, event.data) &&
                        Objects.equals(lastEventId, event.lastEventId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), eventName, data, lastEventId);
    }
}
