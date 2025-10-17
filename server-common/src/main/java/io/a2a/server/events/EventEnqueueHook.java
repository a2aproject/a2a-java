package io.a2a.server.events;

public interface EventEnqueueHook {
    void onEnqueue(EventQueueItem item);
}