package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for asynchronous push notifications of task updates.
 * <p>
 * This record defines the endpoint and authentication details for receiving task event
 * notifications. When configured, the agent will POST task updates (status changes,
 * artifact additions, completions) to the specified URL as they occur, enabling
 * asynchronous workflows without polling.
 * <p>
 * Authentication can be provided via either:
 * <ul>
 *   <li>Simple bearer token in the {@code token} field</li>
 *   <li>More complex authentication via {@link AuthenticationInfo}</li>
 * </ul>
 *
 * @param url the HTTP/HTTPS endpoint URL to receive push notifications (required)
 * @param token optional bearer token for simple authentication
 * @param authentication optional complex authentication configuration
 * @param id optional client-provided identifier for this configuration
 * @see AuthenticationInfo for authentication details
 * @see TaskPushNotificationConfig for task-specific bindings
 * @see MessageSendConfiguration for configuring push notifications on message send
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record PushNotificationConfig(String url, @Nullable String token, @Nullable AuthenticationInfo authentication, @Nullable String id) {

    /**
     * Compact constructor for validation.
     * Validates that the URL is not null.
     *
     * @param url the notification endpoint URL
     * @param token optional bearer token
     * @param authentication optional authentication info
     * @param id optional configuration identifier
     */
    public PushNotificationConfig {
        Assert.checkNotNullParam("url", url);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new Builder initialized with values from an existing PushNotificationConfig.
     *
     * @param config the PushNotificationConfig to copy values from
     * @return the builder
     */
    public static Builder builder(PushNotificationConfig config) {
        return new Builder(config);
    }

    /**
     * Builder for constructing {@link PushNotificationConfig} instances.
     * <p>
     * Provides a fluent API for building push notification configurations with optional
     * authentication and identification.
     */
    public static class Builder {
        private @Nullable String url;
        private @Nullable String token;
        private @Nullable AuthenticationInfo authentication;
        private @Nullable String id;

        /** Creates an empty builder. */
        private Builder() {
        }

        /**
         * Creates a builder initialized from an existing configuration.
         *
         * @param notificationConfig the configuration to copy
         */
        private Builder(PushNotificationConfig notificationConfig) {
            this.url = notificationConfig.url;
            this.token = notificationConfig.token;
            this.authentication = notificationConfig.authentication;
            this.id = notificationConfig.id;
        }

        /**
         * Sets the push notification endpoint URL.
         *
         * @param url the HTTP/HTTPS endpoint (required)
         * @return this builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the bearer token for simple authentication.
         *
         * @param token the bearer token
         * @return this builder
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets complex authentication information.
         *
         * @param authenticationInfo the authentication configuration
         * @return this builder
         */
        public Builder authentication(AuthenticationInfo authenticationInfo) {
            this.authentication = authenticationInfo;
            return this;
        }

        /**
         * Sets the client-provided configuration identifier.
         *
         * @param id the configuration ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Builds the {@link PushNotificationConfig}.
         *
         * @return a new push notification configuration
         * @throws IllegalArgumentException if url is null
         */
        public PushNotificationConfig build() {
            return new PushNotificationConfig(Assert.checkNotNullParam("url", url), token, authentication, id);
        }
    }
}
