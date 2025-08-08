package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Represents the parameters for sending a message in the A2A protocol.
 * This class encapsulates all the necessary information required to send a message,
 * including the message content, configuration settings, and metadata.
 *
 * <p>The parameters include:</p>
 * <ul>
 *   <li>The actual message content to be sent</li>
 *   <li>Configuration settings that control message delivery behavior</li>
 *   <li>Additional metadata associated with the message</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageSendParams(Message message, MessageSendConfiguration configuration,
                                Map<String, Object> metadata) {

    /**
     * Compact constructor that validates the message send parameters.
     * 
     * @throws IllegalArgumentException if message is null
     */
    public MessageSendParams {
        Assert.checkNotNullParam("message", message);
    }

    /**
     * Builder class for constructing MessageSendParams instances.
     * Provides a fluent API for setting message parameters.
     */
    public static class Builder {
        /** The message content to be sent */
        Message message;
        
        /** The message send configuration */
        MessageSendConfiguration configuration;
        
        /** Additional metadata for the message */
        Map<String, Object> metadata;

        /**
         * Sets the message content to be sent.
         * 
         * @param message the message content (required)
         * @return this builder instance for method chaining
         */
        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the configuration for message sending.
         * 
         * @param configuration the message send configuration (optional)
         * @return this builder instance for method chaining
         */
        public Builder configuration(MessageSendConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Sets the metadata for the message.
         * 
         * @param metadata additional metadata associated with the message (optional)
         * @return this builder instance for method chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds and returns a new MessageSendParams instance.
         * 
         * @return a new MessageSendParams with the specified parameters
         * @throws IllegalArgumentException if message is null
         */
        public MessageSendParams build() {
            return new MessageSendParams(message, configuration, metadata);
        }
    }
}
