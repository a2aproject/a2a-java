package io.a2a.spec;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Configuration options for {@code message/send} and {@code message/stream} requests.
 * <p>
 * This record defines how the agent should process a message request, including output format
 * preferences, conversation history context, push notification settings, and blocking behavior.
 * <p>
 * All fields are optional and have sensible defaults when not specified.
 *
 * @param acceptedOutputModes list of output modes the client can handle (e.g., "text", "audio")
 * @param historyLength number of previous messages to include in conversation context (must be non-negative)
 * @param pushNotificationConfig configuration for asynchronous push notifications when task state changes
 * @param blocking whether the request should block until task completion (defaults to false)
 * @see MessageSendParams for the parameters that use this configuration
 * @see PushNotificationConfig for push notification options
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record MessageSendConfiguration(List<String> acceptedOutputModes, Integer historyLength,
        PushNotificationConfig pushNotificationConfig, Boolean blocking) {

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

        List<String> acceptedOutputModes;
        Integer historyLength;
        PushNotificationConfig pushNotificationConfig;
        Boolean blocking = false;

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
         *
         * @param pushNotificationConfig configuration for push notifications
         * @return this builder
         */
        public Builder pushNotificationConfig(@Nullable PushNotificationConfig pushNotificationConfig) {
            this.pushNotificationConfig = pushNotificationConfig;
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
         * Sets whether the request should block until completion.
         *
         * @param blocking true to block until task completes, false for fire-and-forget
         * @return this builder
         */
        public Builder blocking(@NonNull Boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        /**
         * Builds the {@link MessageSendConfiguration}.
         *
         * @return a new message send configuration instance
         */
        public MessageSendConfiguration build() {
            return new MessageSendConfiguration(acceptedOutputModes, historyLength, pushNotificationConfig, blocking);
        }
    }
}
