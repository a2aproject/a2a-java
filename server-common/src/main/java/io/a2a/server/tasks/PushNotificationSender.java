package io.a2a.server.tasks;

import io.a2a.spec.Task;

/**
 * Interface for sending push notifications for tasks.
 */
public interface PushNotificationSender {

    /**
     * Sends a push notification containing the latest task state.
     * @param task the task
     */
    void sendNotification(Task task);
}
