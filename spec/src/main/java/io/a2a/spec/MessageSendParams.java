package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for sending a message to an agent in the A2A Protocol.
 * <p>
 * This record encapsulates the message content, optional configuration, and metadata for
 * agent task requests. It is used to define what message to send and how to process it.
 * <p>
 * The message can create a new task, continue an existing task (if it contains a task ID),
 * or restart a task depending on the agent's implementation and the message context.
 *
 * @param message the message to send to the agent (required)
 * @param configuration optional configuration for message processing behavior
 * @param metadata optional arbitrary key-value metadata for the request
 * @param tenant optional tenant identifier provided as a path parameter; defaults to empty string if not specified
 * @see MessageSendConfiguration for available configuration options
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record MessageSendParams(Message message, @Nullable MessageSendConfiguration configuration,
                                @Nullable Map<String, Object> metadata, @Nullable String tenant) {

    /**
     * Compact constructor for validation and normalization.
     * Validates that {@code message} is not null and normalizes
     * {@code tenant} to an empty string if null.
     */
    public MessageSendParams {
        Assert.checkNotNullParam("message", message);
        tenant = tenant == null ? "" : tenant;
    }

    /**
     * Convenience constructor that sets tenant to empty string by default.
     *
     * @param message the message to send (required)
     * @param configuration optional configuration for message processing
     * @param metadata optional metadata
     */
     public MessageSendParams(Message message, @Nullable MessageSendConfiguration configuration, @Nullable Map<String, Object> metadata) {
        this(message, configuration, metadata, "");
    }

    /**
     * Creates a new {@link Builder} for constructing {@link MessageSendParams}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link MessageSendParams} instances.
     * <p>
     * Provides a fluent API for building message send parameters with optional
     * configuration and metadata.
     */
    public static class Builder {
        @Nullable Message message;
        @Nullable MessageSendConfiguration configuration;
        @Nullable Map<String, Object> metadata;
        @Nullable String tenant;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the message to send to the agent.
         *
         * @param message the message (required)
         * @return this builder
         */
        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the optional configuration for message processing.
         *
         * @param configuration the message send configuration
         * @return this builder
         */
        public Builder configuration(@Nullable MessageSendConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Sets optional metadata for the request.
         *
         * @param metadata arbitrary key-value metadata
         * @return this builder
         */
        public Builder metadata(@Nullable Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets optional tenant for the request.
         *
         * @param tenant optional tenant identifier
         * @return this builder
         */
        public Builder tenant(@Nullable String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Builds the {@link MessageSendParams}.
         *
         * @return a new message send parameters instance
         * @throws IllegalArgumentException if message is null
         */
        public MessageSendParams build() {
            return new MessageSendParams(
                    Assert.checkNotNullParam("message", message),
                    configuration,
                    metadata,
                    tenant);
        }
    }
}
