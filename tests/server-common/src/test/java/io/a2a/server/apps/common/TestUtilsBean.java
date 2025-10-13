package io.a2a.server.apps.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Event;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;

/**
 * Contains utilities to interact with the server side for the tests.
 * The intent for this bean is to be exposed via REST.
 *
 * <p>There is a Quarkus implementation in {@code A2ATestRoutes} which shows the contract for how to
 * expose it via REST. For other REST frameworks, you will need to provide an implementation that works in a similar
 * way to {@code A2ATestRoutes}.</p>
 */
@ApplicationScoped
public class TestUtilsBean {

    @Inject
    TaskStore taskStore;

    @Inject
    QueueManager queueManager;

    @Inject
    PushNotificationConfigStore pushNotificationConfigStore;

    public void saveTask(Task task) {
        taskStore.save(task);
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

    public void deleteTaskPushNotificationConfig(String taskId, String configId) {
        pushNotificationConfigStore.deleteInfo(taskId, configId);
    }

    public void saveTaskPushNotificationConfig(String taskId, PushNotificationConfig notificationConfig) {
        pushNotificationConfigStore.setInfo(taskId, notificationConfig);
    }
}
