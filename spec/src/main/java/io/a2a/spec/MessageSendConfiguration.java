package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Represents the configuration parameters for sending a message in the A2A protocol.
 * This configuration controls various aspects of message delivery and response handling.
 *
 * <p>The configuration includes:</p>
 * <ul>
 *   <li>Accepted output modes (MIME types) that the client can handle</li>
 *   <li>History length for retrieving previous messages in the conversation</li>
 *   <li>Push notification settings for asynchronous updates</li>
 *   <li>Blocking behavior for synchronous vs asynchronous processing</li>
 * </ul>
 *
 * <p><strong>Important behavioral notes:</strong></p>
 * <ul>
 *   <li>If {@code blocking} is true, {@code pushNotification} is ignored</li>
 *   <li>Both {@code blocking} and {@code pushNotification} are ignored in streaming interactions</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageSendConfiguration(List<String> acceptedOutputModes, Integer historyLength,
                                       PushNotificationConfig pushNotification, boolean blocking) {

    /**
     * Compact constructor that validates the configuration parameters.
     * 
     * @throws IllegalArgumentException if acceptedOutputModes is null or historyLength is negative
     */
    public MessageSendConfiguration {
        Assert.checkNotNullParam("acceptedOutputModes", acceptedOutputModes);
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    /**
     * Builder class for constructing MessageSendConfiguration instances.
     * Provides a fluent API for setting configuration parameters.
     */
    public static class Builder {
        /** List of accepted output MIME types */
        List<String> acceptedOutputModes;
        
        /** Number of messages to retrieve from history */
        Integer historyLength;
        
        /** Push notification configuration */
        PushNotificationConfig pushNotification;
        
        /** Whether to use blocking behavior */
        boolean blocking;

        /**
         * Sets the list of MIME types that the client accepts as output.
         * 
         * @param acceptedOutputModes list of accepted MIME types (required)
         * @return this builder instance for method chaining
         */
        public Builder acceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = acceptedOutputModes;
            return this;
        }

        /**
         * Sets the push notification configuration for asynchronous updates.
         * 
         * @param pushNotification the push notification configuration (ignored if blocking is true)
         * @return this builder instance for method chaining
         */
        public Builder pushNotification(PushNotificationConfig pushNotification) {
            this.pushNotification = pushNotification;
            return this;
        }

        /**
         * Sets the number of recent messages to retrieve from conversation history.
         * 
         * @param historyLength the number of messages to retrieve (null for no history, must be non-negative)
         * @return this builder instance for method chaining
         */
        public Builder historyLength(Integer historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        /**
         * Sets whether the client should use blocking behavior.
         * 
         * @param blocking true for synchronous blocking behavior, false for asynchronous handling
         * @return this builder instance for method chaining
         */
        public Builder blocking(boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        /**
         * Builds and returns a new MessageSendConfiguration instance.
         * 
         * @return a new MessageSendConfiguration with the specified parameters
         * @throws IllegalArgumentException if the configuration is invalid
         */
        public MessageSendConfiguration build() {
            return new MessageSendConfiguration(acceptedOutputModes, historyLength, pushNotification, blocking);
        }
    }
}
