package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.util.Assert;

/**
 * Defines the configuration for setting up push notifications for task updates.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushNotificationConfig(String url, String token, PushNotificationAuthenticationInfo authentication, String id) {
    public static final TypeReference<PushNotificationConfig> TYPE_REFERENCE = new TypeReference<>() {};

    public PushNotificationConfig {
        Assert.checkNotNullParam("url", url);
    }

    public static class Builder {
        private String url;
        private String token;
        private PushNotificationAuthenticationInfo authentication;
        private String id;

        public Builder() {
        }

        public Builder(PushNotificationConfig notificationConfig) {
            this.url = notificationConfig.url;
            this.token = notificationConfig.token;
            this.authentication = notificationConfig.authentication;
            this.id = notificationConfig.id;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder authenticationInfo(PushNotificationAuthenticationInfo authenticationInfo) {
            this.authentication = authenticationInfo;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public PushNotificationConfig build() {
            return new PushNotificationConfig(url, token, authentication, id);
        }
    }
}
