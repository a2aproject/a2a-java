package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Defines the configuration for setting up push notifications for task updates.
 */
public record PushNotificationConfig(String url, String token, PushNotificationAuthenticationInfo authentication, String id) {

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
