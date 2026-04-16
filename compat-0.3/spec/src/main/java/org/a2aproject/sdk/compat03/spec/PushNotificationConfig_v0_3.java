package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;

/**
 * Defines the configuration for setting up push notifications for task updates.
 */
public record PushNotificationConfig_v0_3(String url, String token, PushNotificationAuthenticationInfo_v0_3 authentication, String id) {

    public PushNotificationConfig_v0_3 {
        Assert.checkNotNullParam("url", url);
    }

    public static class Builder {
        private String url;
        private String token;
        private PushNotificationAuthenticationInfo_v0_3 authentication;
        private String id;

        public Builder() {
        }

        public Builder(PushNotificationConfig_v0_3 notificationConfig) {
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

        public Builder authenticationInfo(PushNotificationAuthenticationInfo_v0_3 authenticationInfo) {
            this.authentication = authenticationInfo;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public PushNotificationConfig_v0_3 build() {
            return new PushNotificationConfig_v0_3(url, token, authentication, id);
        }
    }
}
