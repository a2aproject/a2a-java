package org.a2aproject.sdk.compat03.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.a2aproject.sdk.util.Assert;

/**
 * A container associating a push notification configuration with a specific task.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskPushNotificationConfig(String taskId, PushNotificationConfig pushNotificationConfig) {

    public TaskPushNotificationConfig {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("pushNotificationConfig", pushNotificationConfig);
    }
}
