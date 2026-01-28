package io.a2a.server.events;

import java.util.Objects;

record MainEventBusContext(String taskId, EventQueue.MainQueue eventQueue, EventQueueItem eventQueueItem) {
    MainEventBusContext {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        Objects.requireNonNull(eventQueue, "eventQueue cannot be null");
        Objects.requireNonNull(eventQueueItem, "eventQueueItem cannot be null");
    }
}
