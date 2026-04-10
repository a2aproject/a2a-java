package org.a2aproject.sdk.compat03.spec;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines configuration options for a `message/send` or `message/stream` request.
 */
public record MessageSendConfiguration_v0_3(List<String> acceptedOutputModes, Integer historyLength,
                                            PushNotificationConfig_v0_3 pushNotificationConfig, Boolean blocking) {

    public MessageSendConfiguration_v0_3 {
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    public static class Builder {

        List<String> acceptedOutputModes;
        Integer historyLength;
        PushNotificationConfig_v0_3 pushNotificationConfig;
        Boolean blocking = true;

        public Builder acceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = acceptedOutputModes;
            return this;
        }

        public Builder pushNotificationConfig(@Nullable PushNotificationConfig_v0_3 pushNotificationConfig) {
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

        public MessageSendConfiguration_v0_3 build() {
            return new MessageSendConfiguration_v0_3(acceptedOutputModes, historyLength, pushNotificationConfig, blocking);
        }
    }
}
