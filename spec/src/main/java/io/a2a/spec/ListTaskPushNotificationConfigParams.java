package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Parameters for listing all push notification configurations for a task.
 * <p>
 * This record specifies which task's push notification configurations to list, returning
 * all configured notification endpoints for that task.
 *
 * @param id the task identifier (required)
 * @param metadata optional arbitrary key-value metadata for the request
 * @see ListTaskPushNotificationConfigRequest for the request using these parameters
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record ListTaskPushNotificationConfigParams(String id, Map<String, Object> metadata) {

    public ListTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
    }

    public ListTaskPushNotificationConfigParams(String id) {
        this(id, null);
    }
}
