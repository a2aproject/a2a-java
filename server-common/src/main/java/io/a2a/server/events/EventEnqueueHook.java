package io.a2a.server.events;

/**
 * Hook interface for event queue enqueue operations.
 * Implementations can be notified when items are enqueued to the event queue,
 * allowing for custom behavior such as event replication or logging.
 */
public interface EventEnqueueHook {
    /**
     * Called when an item is enqueued to the event queue.
     *
     * @param item the event queue item being enqueued
     */
    void onEnqueue(EventQueueItem item);
}