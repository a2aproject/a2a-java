package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.util.Assert;

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
 *   <li>More complex authentication via {@link PushNotificationAuthenticationInfo}</li>
 * </ul>
 *
 * @param url the HTTP/HTTPS endpoint URL to receive push notifications (required)
 * @param token optional bearer token for simple authentication
 * @param authentication optional complex authentication configuration
 * @param id optional client-provided identifier for this configuration
 * @see PushNotificationAuthenticationInfo for authentication details
 * @see TaskPushNotificationConfig for task-specific bindings
 * @see MessageSendConfiguration for configuring push notifications on message send
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushNotificationConfig(String url, String token, PushNotificationAuthenticationInfo authentication, String id) {
    public static final TypeReference<PushNotificationConfig> TYPE_REFERENCE = new TypeReference<>() {};

    public PushNotificationConfig {
        Assert.checkNotNullParam("url", url);
    }

    /**
     * Builder for constructing {@link PushNotificationConfig} instances.
     * <p>
     * Provides a fluent API for building push notification configurations with optional
     * authentication and identification.
     */
    public static class Builder {
        private String url;
        private String token;
        private PushNotificationAuthenticationInfo authentication;
        private String id;

        /** Creates an empty builder. */
        public Builder() {
        }

        /**
         * Creates a builder initialized from an existing configuration.
         *
         * @param notificationConfig the configuration to copy
         */
        public Builder(PushNotificationConfig notificationConfig) {
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
        public Builder authenticationInfo(PushNotificationAuthenticationInfo authenticationInfo) {
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
            return new PushNotificationConfig(url, token, authentication, id);
        }
    }
}
