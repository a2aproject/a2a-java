package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Parameters for sending a message to an agent in the A2A Protocol.
 * <p>
 * This record encapsulates the message content, optional configuration, and metadata for
 * agent task requests. It is used by both {@link SendMessageRequest} and
 * {@link SendStreamingMessageRequest} to define what message to send and how to process it.
 * <p>
 * The message can create a new task, continue an existing task (if it contains a task ID),
 * or restart a task depending on the agent's implementation and the message context.
 *
 * @param message the message to send to the agent (required)
 * @param configuration optional configuration for message processing behavior
 * @param metadata optional arbitrary key-value metadata for the request
 * @see SendMessageRequest for non-streaming message delivery
 * @see SendStreamingMessageRequest for streaming message delivery
 * @see MessageSendConfiguration for available configuration options
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageSendParams(Message message, MessageSendConfiguration configuration,
                                Map<String, Object> metadata) {

    public MessageSendParams {
        Assert.checkNotNullParam("message", message);
    }

    /**
     * Builder for constructing {@link MessageSendParams} instances.
     * <p>
     * Provides a fluent API for building message send parameters with optional
     * configuration and metadata.
     */
    public static class Builder {
        Message message;
        MessageSendConfiguration configuration;
        Map<String, Object> metadata;

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
        public Builder configuration(MessageSendConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Sets optional metadata for the request.
         *
         * @param metadata arbitrary key-value metadata
         * @return this builder
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds the {@link MessageSendParams}.
         *
         * @return a new message send parameters instance
         * @throws IllegalArgumentException if message is null
         */
        public MessageSendParams build() {
            return new MessageSendParams(message, configuration, metadata);
        }
    }
}
