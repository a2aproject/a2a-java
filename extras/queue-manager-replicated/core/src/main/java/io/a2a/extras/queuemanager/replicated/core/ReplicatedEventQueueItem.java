package io.a2a.extras.queuemanager.replicated.core;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import io.a2a.server.events.EventQueueItem;
import io.a2a.spec.Event;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.StreamingEventKind;

public class ReplicatedEventQueueItem implements EventQueueItem {
    private String taskId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private StreamingEventKind event;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private JSONRPCError error;

    private boolean closedEvent;

    // Default constructor for JSON deserialization
    public ReplicatedEventQueueItem() {
    }

    // Constructor for creating from A2A StreamingEventKind objects
    public ReplicatedEventQueueItem(String taskId, StreamingEventKind event) {
        this.taskId = taskId;
        this.event = event;
        this.error = null;
    }

    // Constructor for creating from A2A JSONRPCError objects
    public ReplicatedEventQueueItem(String taskId, JSONRPCError error) {
        this.taskId = taskId;
        this.event = null;
        this.error = error;
    }

    // Backward compatibility constructor for generic Event objects
    public ReplicatedEventQueueItem(String taskId, Event event) {
        this.taskId = taskId;
        if (event instanceof io.a2a.server.events.QueueClosedEvent) {
            this.event = null;
            this.error = null;
            this.closedEvent = true;
        } else if (event instanceof StreamingEventKind streamingEvent) {
            this.event = streamingEvent;
            this.error = null;
            this.closedEvent = false;
        } else if (event instanceof JSONRPCError jsonRpcError) {
            this.event = null;
            this.error = jsonRpcError;
            this.closedEvent = false;
        } else {
            throw new IllegalArgumentException("Event must be StreamingEventKind, JSONRPCError, or QueueClosedEvent, got: " + event.getClass());
        }
    }


    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * Get the StreamingEventKind event field (for JSON serialization).
     * @return the StreamingEventKind event or null
     */
    @JsonGetter("event")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public StreamingEventKind getStreamingEvent() {
        return event;
    }

    @JsonSetter("event")
    public void setEvent(StreamingEventKind event) {
        this.event = event;
        this.error = null; // Clear error when setting event
    }

    /**
     * Get the JSONRPCError field (for JSON serialization).
     * @return the JSONRPCError or null
     */
    @JsonGetter("error")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public JSONRPCError getErrorObject() {
        return error;
    }

    @JsonSetter("error")
    public void setError(JSONRPCError error) {
        this.error = error;
        this.event = null; // Clear event when setting error
    }

    /**
     * Get the contained event as the generic Event interface (implements EventQueueItem).
     * This is the method required by the EventQueueItem interface.
     * @return the event (StreamingEventKind, JSONRPCError, or QueueClosedEvent) or null if none is set
     */
    @JsonIgnore
    @Override
    public Event getEvent() {
        if (closedEvent) {
            return new io.a2a.server.events.QueueClosedEvent(taskId);
        }
        if (event != null) {
            return event;
        }
        return error;
    }

    /**
     * Indicates this is a replicated event (implements EventQueueItem).
     * @return always true for replicated events
     */
    @JsonIgnore
    @Override
    public boolean isReplicated() {
        return true;
    }

    /**
     * Check if this ReplicatedEvent contains an event (vs an error).
     * @return true if it contains a StreamingEventKind event
     */
    public boolean hasEvent() {
        return event != null;
    }

    /**
     * Check if this ReplicatedEvent contains an error.
     * @return true if it contains a JSONRPCError
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Check if this is a QueueClosedEvent (poison pill).
     * For JSON serialization.
     * @return true if this is a queue closed event
     */
    @JsonGetter("closedEvent")
    public boolean isClosedEvent() {
        return closedEvent;
    }

    /**
     * Set the closed event flag (for JSON deserialization).
     * @param closedEvent true if this is a queue closed event
     */
    @JsonSetter("closedEvent")
    public void setClosedEvent(boolean closedEvent) {
        this.closedEvent = closedEvent;
        if (closedEvent) {
            this.event = null;
            this.error = null;
        }
    }

    @Override
    public String toString() {
        return "ReplicatedEventQueueItem{" +
                "taskId='" + taskId + '\'' +
                ", event=" + event +
                ", error=" + error +
                ", closedEvent=" + closedEvent +
                '}';
    }
}