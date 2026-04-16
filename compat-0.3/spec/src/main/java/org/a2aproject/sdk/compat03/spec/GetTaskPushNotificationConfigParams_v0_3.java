package org.a2aproject.sdk.compat03.spec;

import java.util.Map;


import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for fetching a pushNotificationConfiguration associated with a Task.
 */
public record GetTaskPushNotificationConfigParams_v0_3(String id, @Nullable String pushNotificationConfigId, @Nullable Map<String, Object> metadata) {

    public GetTaskPushNotificationConfigParams_v0_3 {
        Assert.checkNotNullParam("id", id);
    }

    public GetTaskPushNotificationConfigParams_v0_3(String id) {
        this(id, null, null);
    }

    public GetTaskPushNotificationConfigParams_v0_3(String id, String pushNotificationConfigId) {
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

        public GetTaskPushNotificationConfigParams_v0_3 build() {
            return new GetTaskPushNotificationConfigParams_v0_3(id, pushNotificationConfigId, metadata);
        }
    }
}
