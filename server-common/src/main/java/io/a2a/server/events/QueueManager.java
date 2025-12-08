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
     * Creates a base EventQueueBuilder with standard configuration for this QueueManager.
     * This method provides the foundation for creating event queues with proper configuration
     * (MainEventBus, TaskStateProvider, cleanup callbacks, etc.).
     * <p>
     * QueueManager implementations that use custom factories can call this method directly
     * to get the base builder without going through the factory (which could cause infinite
     * recursion if the factory delegates back to getEventQueueBuilder()).
     * </p>
     * <p>
     * Callers can then add additional configuration (hooks, callbacks) before building the queue.
     * </p>
     *
     * @param taskId the task ID for the queue
     * @return a builder with base configuration specific to this QueueManager implementation
     */
    default EventQueue.EventQueueBuilder createBaseEventQueueBuilder(String taskId) {
        return EventQueue.builder().taskId(taskId);
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
