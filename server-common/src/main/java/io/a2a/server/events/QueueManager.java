package io.a2a.server.events;

public interface QueueManager {
    void add(String taskId, EventQueue queue);

    EventQueue get(String taskId);

    EventQueue tap(String taskId);

    void close(String taskId);

    EventQueue createOrTap(String taskId);

    void awaitQueuePollerStart(EventQueue eventQueue) throws InterruptedException;
}
