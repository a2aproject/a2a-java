package io.a2a.spec;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines configuration options for a {@code message/send} or {@code message/stream} request.
 *
 * @param acceptedOutputModes the output content modes the client accepts
 * @param historyLength optional maximum number of history messages to include
 * @param pushNotificationConfig optional push notification configuration for task updates
 * @param blocking whether the request should block until the task completes
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
