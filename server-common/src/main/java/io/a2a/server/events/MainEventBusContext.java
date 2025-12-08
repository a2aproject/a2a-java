package io.a2a.server.events;

import java.util.Objects;

class MainEventBusContext {
    private final String taskId;
    private final EventQueue eventQueue;
    private final EventQueueItem eventQueueItem;

    public MainEventBusContext(String taskId, EventQueue eventQueue, EventQueueItem eventQueueItem) {
        this.taskId = Objects.requireNonNull(taskId, "taskId cannot be null");
        this.eventQueue = Objects.requireNonNull(eventQueue, "eventQueue cannot be null");
        this.eventQueueItem = Objects.requireNonNull(eventQueueItem, "eventQueueItem cannot be null");
    }

    public String taskId() {
        return taskId;
    }

    public EventQueue eventQueue() {
        return eventQueue;
    }

    public EventQueueItem eventQueueItem() {
        return eventQueueItem;
    }

}
