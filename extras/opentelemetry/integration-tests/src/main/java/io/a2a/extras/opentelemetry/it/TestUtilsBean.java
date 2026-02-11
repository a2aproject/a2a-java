package io.a2a.extras.opentelemetry.it;

import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Event;
import io.a2a.spec.Task;
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
