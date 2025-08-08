package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents the configuration for push notifications in the A2A protocol.
 * This record defines the settings and parameters required to enable and configure
 * push notification delivery for agent-to-application communication.
 *
 * <p>Push notifications provide a mechanism for agents to proactively notify client
 * applications about important events, task updates, or status changes without
 * requiring the client to continuously poll for updates. This improves efficiency
 * and user experience by enabling real-time communication.</p>
 *
 * <p>The configuration includes:</p>
 * <ul>
 *   <li><strong>Authentication Information:</strong> Credentials and schemes for push services</li>
 *   <li><strong>Delivery Settings:</strong> How and when notifications should be sent</li>
 *   <li><strong>Service Configuration:</strong> Provider-specific settings and endpoints</li>
 * </ul>
 *
 * <p>This implementation supports various push notification providers and protocols,
 * allowing flexible integration with different client platforms and notification systems.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushNotificationConfig(String url, String token, PushNotificationAuthenticationInfo authentication, String id) {

    public PushNotificationConfig {
        Assert.checkNotNullParam("url", url);
    }

    /**
     * Builder class for constructing PushNotificationConfig instances.
     * Provides a fluent API for configuring push notification settings.
     */
    public static class Builder {
        /** The URL endpoint for push notification delivery */
        private String url;
        /** The authentication token for push notification services */
        private String token;
        /** The authentication information for push notifications */
        private PushNotificationAuthenticationInfo authentication;
        /** Unique identifier for this push notification configuration */
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

        /**
         * Sets the authentication token for push notification services.
         * 
         * @param token the authentication token
         * @return this builder instance for method chaining
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets the authentication information for push notifications.
         * This information is used to authenticate with push notification services
         * when delivering notifications to client applications.
         * 
         * @param authenticationInfo the authentication configuration for push notifications
         * @return this builder instance for method chaining
         */
        public Builder authenticationInfo(PushNotificationAuthenticationInfo authenticationInfo) {
            this.authentication = authenticationInfo;
            return this;
        }

        /**
         * Sets the unique identifier for this push notification configuration.
         * 
         * @param id the unique identifier
         * @return this builder instance for method chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Builds and returns a new PushNotificationConfig instance.
         * 
         * @return a new PushNotificationConfig with the configured settings
         */
        public PushNotificationConfig build() {
            return new PushNotificationConfig(url, token, authentication, id);
        }
    }
}
