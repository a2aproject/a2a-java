package io.a2a.spec;

import java.util.Map;


import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for retrieving push notification configuration for a specific task.
 * <p>
 * This record specifies which task's push notification configuration to retrieve, with
 * an optional filter by configuration ID if multiple configurations exist for the task.
 *
 * @param id the task identifier (required)
 * @param pushNotificationConfigId optional specific configuration ID to retrieve
 * @param metadata optional arbitrary key-value metadata for the request
 * @see GetTaskPushNotificationConfigRequest for the request using these parameters
 * @see TaskPushNotificationConfig for the returned configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record GetTaskPushNotificationConfigParams(String id, @Nullable String pushNotificationConfigId, @Nullable Map<String, Object> metadata) {

    public GetTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
    }

    public GetTaskPushNotificationConfigParams(String id) {
        this(id, null, null);
    }

    public GetTaskPushNotificationConfigParams(String id, String pushNotificationConfigId) {
        this(id, pushNotificationConfigId, null);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        String id;
        String pushNotificationConfigId;
        Map<String, Object> metadata;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

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

        public GetTaskPushNotificationConfigParams build() {
            return new GetTaskPushNotificationConfigParams(id, pushNotificationConfigId, metadata);
        }
    }
}
