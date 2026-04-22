package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * A container associating a push notification configuration with a specific task.
 *
 * @param taskId the ID of the task this configuration is associated with
 * @param pushNotificationConfig the push notification configuration for the task
 */
public record TaskPushNotificationConfig(String taskId, PushNotificationConfig pushNotificationConfig) {

    public TaskPushNotificationConfig {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("pushNotificationConfig", pushNotificationConfig);
    }
}
