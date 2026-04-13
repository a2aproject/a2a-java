package org.a2aproject.sdk.compat03.spec;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines configuration options for a `message/send` or `message/stream` request.
 */
public record MessageSendConfiguration(List<String> acceptedOutputModes, Integer historyLength,
        PushNotificationConfig pushNotificationConfig, Boolean blocking) {

    public MessageSendConfiguration {
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    public static class Builder {

        List<String> acceptedOutputModes;
        Integer historyLength;
        PushNotificationConfig pushNotificationConfig;
        Boolean blocking = true;

        public Builder acceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = acceptedOutputModes;
            return this;
        }

        public Builder pushNotificationConfig(@Nullable PushNotificationConfig pushNotificationConfig) {
            this.pushNotificationConfig = pushNotificationConfig;
            return this;
        }

        public Builder historyLength(@Nullable Integer historyLength) {
            if (historyLength != null && historyLength < 0) {
                throw new IllegalArgumentException("Invalid history length");
            }
            this.historyLength = historyLength;
            return this;
        }

        public Builder blocking(@NonNull Boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        public MessageSendConfiguration build() {
            return new MessageSendConfiguration(acceptedOutputModes, historyLength, pushNotificationConfig, blocking);
        }
    }
}
