package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * A container associating a push notification configuration with a specific task.
 */
public record TaskPushNotificationConfig(String taskId, PushNotificationConfig pushNotificationConfig) {

    public TaskPushNotificationConfig {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("pushNotificationConfig", pushNotificationConfig);
    }
}
