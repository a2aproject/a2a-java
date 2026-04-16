package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * A container associating a push notification configuration with a specific task.
 */
public record TaskPushNotificationConfig_v0_3(String taskId, PushNotificationConfig_v0_3 pushNotificationConfig) {

    public TaskPushNotificationConfig_v0_3 {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("pushNotificationConfig", pushNotificationConfig);
    }
}
