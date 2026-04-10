package org.a2aproject.sdk.spec;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Push notification configuration associated with a specific task.
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
 * <p>
 * Used for managing task-specific push notification settings via the push notification
 * management methods ({@code tasks/pushNotificationConfig/set}, {@code tasks/pushNotificationConfig/get}, etc.).
 *
 * @param id unique identifier (e.g. UUID) for this push notification configuration
 * @param taskId the unique identifier of the task to receive push notifications for
 * @param url the HTTP/HTTPS endpoint URL to receive push notifications (required)
 * @param token optional bearer token for simple authentication
 * @param authentication optional complex authentication configuration
 * @param tenant optional tenant identifier, provided as a path parameter
 * @see AuthenticationInfo for authentication details
 * @see MessageSendConfiguration for configuring push notifications on message send
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record TaskPushNotificationConfig(String id, @Nullable String taskId, String url, @Nullable String token,
        @Nullable AuthenticationInfo authentication, @Nullable String tenant) {

    /**
     * Compact constructor for validation.
     * Validates that required parameters are not null.
     *
     * @param id the configuration identifier
     * @param taskId the task identifier
     * @param url the notification endpoint URL
     * @param token optional bearer token
     * @param authentication optional authentication info
     * @param tenant the tenant identifier
     */
    public TaskPushNotificationConfig {
        Assert.checkNotNullParam("id", id);
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
     * Create a new Builder initialized with values from an existing TaskPushNotificationConfig.
     *
     * @param config the TaskPushNotificationConfig to copy values from
     * @return the builder
     */
    public static Builder builder(TaskPushNotificationConfig config) {
        return new Builder(config);
    }

    /**
     * Builder for constructing {@link TaskPushNotificationConfig} instances.
     * <p>
     * Provides a fluent API for building push notification configurations with optional
     * authentication and identification.
     */
    public static class Builder {
        private @Nullable String id;
        private @Nullable String taskId;
        private @Nullable String url;
        private @Nullable String token;
        private @Nullable AuthenticationInfo authentication;
        private @Nullable String tenant;

        /** Creates an empty builder. */
        private Builder() {
        }

        /**
         * Creates a builder initialized from an existing configuration.
         *
         * @param config the configuration to copy
         */
        private Builder(TaskPushNotificationConfig config) {
            this.id = config.id;
            this.taskId = config.taskId;
            this.url = config.url;
            this.token = config.token;
            this.authentication = config.authentication;
            this.tenant = config.tenant;
        }

        /**
         * Sets the configuration identifier.
         *
         * @param id the configuration ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the task identifier.
         *
         * @param taskId the task ID
         * @return this builder
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
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
         * @param authentication the authentication configuration
         * @return this builder
         */
        public Builder authentication(AuthenticationInfo authentication) {
            this.authentication = authentication;
            return this;
        }

        /**
         * Sets the tenant identifier.
         *
         * @param tenant the tenant ID
         * @return this builder
         */
        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Builds the {@link TaskPushNotificationConfig}.
         *
         * @return a new push notification configuration
         * @throws IllegalArgumentException if id or url is null
         */
        public TaskPushNotificationConfig build() {
            return new TaskPushNotificationConfig(
                    Assert.checkNotNullParam("id", id),
                    taskId,
                    Assert.checkNotNullParam("url", url),
                    token,
                    authentication,
                    tenant);
        }
    }
}
