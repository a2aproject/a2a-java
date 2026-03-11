package io.a2a.spec;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Configuration options for {@code message/send} and {@code message/stream} requests.
 * <p>
 * This record defines how the agent should process a message request, including output format
 * preferences, conversation history context, push notification settings, and return behavior.
 * <p>
 * All fields are optional and have sensible defaults when not specified.
 *
 * @param acceptedOutputModes list of output modes the client can handle (e.g., "text", "audio")
 * @param historyLength number of previous messages to include in conversation context (must be non-negative)
 * @param taskPushNotificationConfig configuration for asynchronous push notifications when task state changes.
 *        Task id should be empty when sending this configuration in a SendMessage request.
 * @param returnImmediately whether the operation should return immediately after creating the task (defaults to false)
 * @see MessageSendParams for the parameters that use this configuration
 * @see TaskPushNotificationConfig for push notification options
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record MessageSendConfiguration(@Nullable List<String> acceptedOutputModes, @Nullable Integer historyLength,
        @Nullable TaskPushNotificationConfig taskPushNotificationConfig, Boolean returnImmediately) {

    /**
     * Compact constructor for validation.
     * Validates that historyLength is non-negative if provided.
     *
     * @param acceptedOutputModes list of accepted output modes
     * @param historyLength maximum number of history items
     * @param taskPushNotificationConfig push notification configuration
     * @param returnImmediately whether to return immediately
     * @throws IllegalArgumentException if historyLength is negative
     */
    public MessageSendConfiguration {
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
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
     * Builder for constructing {@link MessageSendConfiguration} instances.
     * <p>
     * Provides a fluent API for configuring message send behavior with sensible defaults.
     */
    public static class Builder {

        @Nullable List<String> acceptedOutputModes;
        @Nullable Integer historyLength;
        @Nullable TaskPushNotificationConfig taskPushNotificationConfig;
        Boolean returnImmediately = false;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the accepted output modes.
         *
         * @param acceptedOutputModes list of output modes the client can handle
         * @return this builder
         */
        public Builder acceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = acceptedOutputModes;
            return this;
        }

        /**
         * Sets the push notification configuration.
         * Task id should be empty when sending this configuration in a SendMessage request.
         *
         * @param taskPushNotificationConfig configuration for push notifications
         * @return this builder
         */
        public Builder taskPushNotificationConfig(@Nullable TaskPushNotificationConfig taskPushNotificationConfig) {
            this.taskPushNotificationConfig = taskPushNotificationConfig;
            return this;
        }

        /**
         * Sets the conversation history length.
         *
         * @param historyLength number of previous messages to include (must be non-negative)
         * @return this builder
         * @throws IllegalArgumentException if historyLength is negative
         */
        public Builder historyLength(@Nullable Integer historyLength) {
            if (historyLength != null && historyLength < 0) {
                throw new IllegalArgumentException("Invalid history length");
            }
            this.historyLength = historyLength;
            return this;
        }

        /**
         * Sets whether the operation should return immediately after creating the task.
         *
         * @param returnImmediately true to return immediately, false to wait for task completion
         * @return this builder
         */
        public Builder returnImmediately(@NonNull Boolean returnImmediately) {
            this.returnImmediately = returnImmediately;
            return this;
        }

        /**
         * Builds the {@link MessageSendConfiguration}.
         *
         * @return a new message send configuration instance
         */
        public MessageSendConfiguration build() {
            return new MessageSendConfiguration(
                    acceptedOutputModes,
                    historyLength,
                    taskPushNotificationConfig,
                    returnImmediately);
        }
    }
}
