package org.a2aproject.sdk.compat03.spec;

import java.util.Map;


import org.a2aproject.sdk.util.Assert;

/**
 * Parameters for removing pushNotificationConfiguration associated with a Task.
 */
public record DeleteTaskPushNotificationConfigParams_v0_3(String id, String pushNotificationConfigId, Map<String, Object> metadata) {

    public DeleteTaskPushNotificationConfigParams_v0_3 {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("pushNotificationConfigId", pushNotificationConfigId);
    }

    public DeleteTaskPushNotificationConfigParams_v0_3(String id, String pushNotificationConfigId) {
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

        public DeleteTaskPushNotificationConfigParams_v0_3 build() {
            return new DeleteTaskPushNotificationConfigParams_v0_3(id, pushNotificationConfigId, metadata);
        }
    }
}
