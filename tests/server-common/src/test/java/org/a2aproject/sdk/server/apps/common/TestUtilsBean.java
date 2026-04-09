package org.a2aproject.sdk.server.apps.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.Task;

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

    public void deleteTaskPushNotificationConfig(String taskId, String configId) {
        pushNotificationConfigStore.deleteInfo(taskId, configId);
    }

    public void saveTaskPushNotificationConfig(String taskId, TaskPushNotificationConfig notificationConfig) {
        pushNotificationConfigStore.setInfo(TaskPushNotificationConfig.builder(notificationConfig).taskId(taskId).build());
    }

    /**
     * Waits for the EventConsumer polling loop to start for the specified task's queue.
     * This ensures the queue is ready to receive and process events.
     *
     * @param taskId the task ID whose queue poller to wait for
     * @throws InterruptedException if interrupted while waiting
     */
    public void awaitQueuePollerStart(String taskId) throws InterruptedException {
        queueManager.awaitQueuePollerStart(queueManager.get(taskId));
    }

    /**
     * Waits for the child queue count to stabilize at the expected value.
     * <p>
     * This method addresses a race condition where EventConsumer polling loops may not have started
     * yet when events are emitted. It waits for the child queue count to match the expected value
     * for 3 consecutive checks (150ms total), ensuring EventConsumers are actively polling and
     * won't miss events.
     * <p>
     * Use this after operations that create child queues (e.g., subscribeToTask, sendMessage) to
     * ensure their EventConsumer polling loops have started before the agent emits events.
     *
     * @param taskId the task ID whose child queues to monitor
     * @param expectedCount the expected number of active child queues
     * @param timeoutMs maximum time to wait in milliseconds
     * @return true if the count stabilized at the expected value, false if timeout occurred
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitChildQueueCountStable(String taskId, int expectedCount, long timeoutMs) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutMs;
        int consecutiveMatches = 0;
        final int requiredMatches = 3; // Count must match 3 times in a row (150ms) to be considered stable

        while (System.currentTimeMillis() < endTime) {
            int count = queueManager.getActiveChildQueueCount(taskId);
            if (count == expectedCount) {
                consecutiveMatches++;
                if (consecutiveMatches >= requiredMatches) {
                    // Count is stable - all child queues exist and haven't closed
                    return true;
                }
            } else {
                consecutiveMatches = 0; // Reset if count changes
            }
            Thread.sleep(50);
        }
        return false;
    }
}
