package io.a2a.extras.queuemanager.replicated.core;

import io.a2a.server.events.EventQueueItem;
import io.a2a.spec.A2AError;
import io.a2a.spec.Event;
import io.a2a.spec.StreamingEventKind;

public class ReplicatedEventQueueItem implements EventQueueItem {
    private String taskId;

    private StreamingEventKind event;

    private A2AError error;

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

    // Constructor for creating from A2A A2AError objects
    public ReplicatedEventQueueItem(String taskId, A2AError error) {
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
        } else if (event instanceof A2AError jsonRpcError) {
            this.event = null;
            this.error = jsonRpcError;
            this.closedEvent = false;
        } else {
            throw new IllegalArgumentException("Event must be StreamingEventKind, A2AError, or QueueClosedEvent, got: " + event.getClass());
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
    public StreamingEventKind getStreamingEvent() {
        return event;
    }

    public void setEvent(StreamingEventKind event) {
        this.event = event;
        this.error = null; // Clear error when setting event
    }

    /**
     * Get the A2AError field (for JSON serialization).
     * @return the A2AError or null
     */
    public A2AError getErrorObject() {
        return error;
    }

    public void setError(A2AError error) {
        this.error = error;
        this.event = null; // Clear event when setting error
    }

    /**
     * Get the contained event as the generic Event interface (implements EventQueueItem).
     * This is the method required by the EventQueueItem interface.
     * @return the event (StreamingEventKind, A2AError, or QueueClosedEvent) or null if none is set
     */
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
     * @return true if it contains a A2AError
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Check if this is a QueueClosedEvent (poison pill).
     * For JSON serialization.
     * @return true if this is a queue closed event
     */
    public boolean isClosedEvent() {
        return closedEvent;
    }

    /**
     * Set the closed event flag (for JSON deserialization).
     * @param closedEvent true if this is a queue closed event
     */
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