package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Parameters for deleting a push notification configuration from a task.
 * <p>
 * This record specifies which task and which specific push notification configuration
 * to remove, allowing cleanup of notification endpoints that are no longer needed.
 *
 * @param id the task identifier (required)
 * @param pushNotificationConfigId the specific configuration ID to delete (required)
 * @param metadata optional arbitrary key-value metadata for the request
 * @see DeleteTaskPushNotificationConfigRequest for the request using these parameters
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeleteTaskPushNotificationConfigParams(String id, String pushNotificationConfigId, Map<String, Object> metadata) {

    public DeleteTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("pushNotificationConfigId", pushNotificationConfigId);
    }

    public DeleteTaskPushNotificationConfigParams(String id, String pushNotificationConfigId) {
        this(id, pushNotificationConfigId, null);
    }

    public static class Builder {
        String id;
        String pushNotificationConfigId;
        Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder pushNotificationConfigId(String pushNotificationConfigId) {
            this.pushNotificationConfigId = pushNotificationConfigId;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public DeleteTaskPushNotificationConfigParams build() {
            return new DeleteTaskPushNotificationConfigParams(id, pushNotificationConfigId, metadata);
        }
    }
}
