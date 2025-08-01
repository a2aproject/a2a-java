package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Represents the configuration of the message to be sent.
 *
 * If {@code blocking} is true, {@code pushNotification} is ignored.
 * Both {@code blocking} and {@code pushNotification} are ignored in streaming interactions.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageSendConfiguration(List<String> acceptedOutputModes, Integer historyLength,
                                       PushNotificationConfig pushNotification, boolean blocking) {

    public MessageSendConfiguration {
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    public static class Builder {
        List<String> acceptedOutputModes;
        Integer historyLength;
        PushNotificationConfig pushNotification;
        boolean blocking;

        public Builder acceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = acceptedOutputModes;
            return this;
        }

        public Builder pushNotification(PushNotificationConfig pushNotification) {
            this.pushNotification = pushNotification;
            return this;
        }

        public Builder historyLength(Integer historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        public Builder blocking(boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        public MessageSendConfiguration build() {
            return new MessageSendConfiguration(acceptedOutputModes, historyLength, pushNotification, blocking);
        }
    }
}
