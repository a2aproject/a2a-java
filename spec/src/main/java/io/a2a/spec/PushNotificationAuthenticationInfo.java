package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents authentication information required for push notification delivery in the A2A protocol.
 * This record contains the credentials and configuration needed to authenticate with
 * push notification services and deliver notifications to client applications.
 *
 * <p>Push notifications in the A2A protocol allow agents to proactively notify clients
 * about task updates, completion status, or other important events without requiring
 * the client to continuously poll for updates.</p>
 *
 * <p>The authentication information typically includes:</p>
 * <ul>
 *   <li>Service-specific credentials (API keys, tokens, certificates)</li>
 *   <li>Configuration parameters for the push notification provider</li>
 *   <li>Delivery preferences and routing information</li>
 * </ul>
 *
 * <p>This implementation supports various push notification services and protocols,
 * ensuring flexible integration with different client platforms and notification systems.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushNotificationAuthenticationInfo(List<String> schemes, String credentials) {

    /**
     * Compact constructor that validates the input parameters.
     * Ensures that the schemes parameter is not null as it's required
     * for proper authentication configuration.
     *
     * @throws IllegalArgumentException if schemes is null
     */
    public PushNotificationAuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }

    /**
     * Builder class for constructing PushNotificationAuthenticationInfo instances.
     * Provides a fluent API for configuring push notification authentication.
     */
    public static class Builder {
        /** The authentication schemes supported for push notifications */
        private List<String> schemes;
        
        /** The credentials for push notification authentication */
        private String credentials;

        /**
         * Sets the authentication schemes supported for push notifications.
         * These schemes define the authentication methods that can be used
         * when delivering push notifications.
         * 
         * @param schemes list of supported authentication scheme names
         * @return this builder instance for method chaining
         */
        public Builder schemes(List<String> schemes) {
            this.schemes = schemes;
            return this;
        }

        /**
         * Sets the credentials for push notification authentication.
         * These credentials are used to authenticate with the push notification
         * service when delivering notifications to clients.
         * 
         * @param credentials the authentication credentials
         * @return this builder instance for method chaining
         */
        public Builder credentials(String credentials) {
            this.credentials = credentials;
            return this;
        }

        /**
         * Builds and returns a new PushNotificationAuthenticationInfo instance.
         * 
         * @return a new PushNotificationAuthenticationInfo with the configured settings
         */
        public PushNotificationAuthenticationInfo build() {
            return new PushNotificationAuthenticationInfo(schemes, credentials);
        }
    }
}
