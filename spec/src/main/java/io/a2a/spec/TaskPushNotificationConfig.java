package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Associates a push notification configuration with a specific task.
 * <p>
 * This record binds a {@link PushNotificationConfig} to a particular task ID, enabling
 * the agent to send asynchronous updates about task progress to the configured endpoint.
 * When task events occur (status changes, artifact updates), the agent will POST updates
 * to the specified notification URL.
 * <p>
 * Used for managing task-specific push notification settings via the push notification
 * management methods ({@code tasks/pushNotificationConfig/set}, {@code tasks/pushNotificationConfig/get}, etc.).
 *
 * @param taskId the unique identifier of the task to receive push notifications for (required)
 * @param pushNotificationConfig the push notification endpoint and authentication configuration (required)
 * @see PushNotificationConfig for notification endpoint details
 * @see SetTaskPushNotificationConfigRequest for setting push notifications
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskPushNotificationConfig(String taskId, PushNotificationConfig pushNotificationConfig) {

    public TaskPushNotificationConfig {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("pushNotificationConfig", pushNotificationConfig);
        Assert.checkNotNullParam("configId", pushNotificationConfig.id());
    }
}
