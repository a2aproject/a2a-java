package io.a2a.server.tasks;

import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;


import io.a2a.spec.PushNotificationConfig;

/**
 * Interface for storing and retrieving push notification configurations for tasks.
 */
public interface PushNotificationConfigStore {

    /**
     * Sets or updates the push notification configuration for a task.
     * @param taskId the task ID
     * @param notificationConfig the push notification configuration
     * @return the potentially updated push notification configuration
     */
    PushNotificationConfig setInfo(String taskId, PushNotificationConfig notificationConfig);

    /**
     * Retrieves the push notification configuration for a task.
     * @param taskId the task ID
     * @return the push notification configurations for a task, or with emty list if not found
     */
    ListTaskPushNotificationConfigResult getInfo(ListTaskPushNotificationConfigParams params);

    /**
     * Deletes the push notification configuration for a task.
     * @param taskId the task ID
     * @param configId the push notification configuration
     */
    void deleteInfo(String taskId, String configId);

}
