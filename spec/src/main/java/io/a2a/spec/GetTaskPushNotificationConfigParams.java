package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for fetching a pushNotificationConfiguration associated with a Task.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
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

        public GetTaskPushNotificationConfigParams build() {
            return new GetTaskPushNotificationConfigParams(id, pushNotificationConfigId, metadata);
        }
    }
}
