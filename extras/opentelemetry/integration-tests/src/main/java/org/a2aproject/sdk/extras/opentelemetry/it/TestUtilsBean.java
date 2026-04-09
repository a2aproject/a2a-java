package org.a2aproject.sdk.extras.opentelemetry.it;

import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.Task;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Test utilities for OpenTelemetry integration tests.
 * Allows direct manipulation of tasks and queues for testing.
 */
@ApplicationScoped
public class TestUtilsBean {

    @Inject
    TaskStore taskStore;

    @Inject
    QueueManager queueManager;

    public void saveTask(Task task) {
        taskStore.save(task, false);
    }

    public Task getTask(String taskId) {
        return taskStore.get(taskId);
    }

    public void deleteTask(String taskId) {
        taskStore.delete(taskId);
    }

    public void ensureQueue(String taskId) {
        queueManager.createOrTap(taskId);
    }

    public void enqueueEvent(String taskId, Event event) {
        queueManager.get(taskId).enqueueEvent(event);
    }

    public int getChildQueueCount(String taskId) {
        return queueManager.getActiveChildQueueCount(taskId);
    }
}
