package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

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
 * @param config the push notification endpoint and authentication configuration (required)
 * @param tenant optional tenant identifier, provided as a path parameter
 * @see PushNotificationConfig for notification endpoint details
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record TaskPushNotificationConfig(String taskId, PushNotificationConfig config, @Nullable String tenant) {

    /**
     * Compact constructor for validation.
     * Validates that required parameters are not null.
     *
     * @param taskId the task identifier
     * @param config the push notification configuration
     * @param tenant the tenant identifier
     */
    public TaskPushNotificationConfig {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("config", config);
        Assert.checkNotNullParam("configId", config.id());
    }
}
