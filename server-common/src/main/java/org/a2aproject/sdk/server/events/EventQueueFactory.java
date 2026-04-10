package org.a2aproject.sdk.server.events;

public interface EventQueueFactory {
    /**
     * Creates an EventQueueBuilder with the specified taskId context.
     * This allows the factory to create queues with task-specific configuration.
     * 
     * @param taskId the task ID for which the queue is being created
     * @return an EventQueueBuilder configured for the specified task
     */
    EventQueue.EventQueueBuilder builder(String taskId);
}