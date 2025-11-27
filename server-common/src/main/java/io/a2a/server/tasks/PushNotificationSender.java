package io.a2a.server.tasks;

import io.a2a.spec.StreamingEventKind;

/**
 * Interface for sending push notifications for tasks.
 */
public interface PushNotificationSender {

    /**
     * Sends a push notification containing payload about a task.
     * @param kind the payload to push
     */
    void sendNotification(StreamingEventKind kind);
}
