package io.a2a.server.events;

import io.a2a.spec.Event;

/**
 * Poison pill event used to signal that a queue has been closed.
 * <p>
 * When a MainQueue is closed on one node, this event is published to Kafka
 * to notify all other nodes consuming replicated events that they should
 * gracefully terminate their event streams for this task.
 * </p>
 * <p>
 * This event implements the "poison pill" pattern - when EventConsumer
 * encounters this event, it throws EventQueueClosedException to terminate
 * the consumption loop and close the stream.
 * </p>
 * <p>
 * Note: This is an internal event that is never sent to end clients.
 * The EventConsumer intercepts this event and terminates the stream
 * before it reaches the client.
 * </p>
 */
public class QueueClosedEvent implements Event {

    private final String taskId;

    public QueueClosedEvent(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public String toString() {
        return "QueueClosedEvent{taskId='" + taskId + "'}";
    }
}
