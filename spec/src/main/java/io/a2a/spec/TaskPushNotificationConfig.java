package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents the configuration for push notifications associated with a specific task.
 * This record binds a task identifier to its corresponding push notification settings,
 * enabling the agent to send real-time notifications about task status changes,
 * progress updates, or completion events.
 *
 * <p>Task push notification configurations are essential for:</p>
 * <ul>
 *   <li>Real-time task status monitoring</li>
 *   <li>Progress tracking for long-running operations</li>
 *   <li>Immediate notification of task completion or failure</li>
 *   <li>Integration with external monitoring and alerting systems</li>
 * </ul>
 *
 * <p>The configuration ensures that clients can receive timely updates about
 * task execution without the need for continuous polling, improving efficiency
 * and user experience.</p>
 *
 * @param taskId the unique identifier of the task to monitor
 * @param pushNotificationConfig the push notification configuration settings
 * @see PushNotificationConfig
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskPushNotificationConfig(String taskId, PushNotificationConfig pushNotificationConfig) {

    /**
     * Compact constructor that validates the required parameters.
     * Ensures that both taskId and pushNotificationConfig are not null.
     *
     * @throws IllegalArgumentException if taskId or pushNotificationConfig is null
     */
    public TaskPushNotificationConfig {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("pushNotificationConfig", pushNotificationConfig);
    }
}
