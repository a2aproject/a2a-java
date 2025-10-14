package io.a2a.server.events;

public interface QueueManager {

    void add(String taskId, EventQueue queue);

    EventQueue get(String taskId);

    EventQueue tap(String taskId);

    void close(String taskId);

    EventQueue createOrTap(String taskId);

    void awaitQueuePollerStart(EventQueue eventQueue) throws InterruptedException;

    default EventQueue.EventQueueBuilder getEventQueueBuilder(String taskId) {
        return EventQueue.builder();
    }

    /**
     * Get the count of active child queues for a given task.
     * Used for testing to verify reference counting mechanism.
     *
     * @param taskId the task ID
     * @return number of active child queues, or -1 if queue doesn't exist
     */
    int getActiveChildQueueCount(String taskId);
}
