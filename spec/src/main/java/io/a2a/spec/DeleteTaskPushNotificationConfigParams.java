package io.a2a.spec;

import java.util.Map;


import io.a2a.util.Assert;

/**
 * Parameters for removing pushNotificationConfiguration associated with a Task.
 */
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
