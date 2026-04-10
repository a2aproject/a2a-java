package org.a2aproject.sdk.compat03.client.config;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import java.util.ArrayList;
import java.util.HashMap;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for the A2A client factory.
 */
public class ClientConfig_v0_3 {

    private final Boolean streaming;
    private final Boolean polling;
    private final Boolean useClientPreference;
    private final List<String> acceptedOutputModes;
    private final @Nullable PushNotificationConfig_v0_3 pushNotificationConfig;
    private final @Nullable Integer historyLength;
    private final Map<String, Object> metadata;

    private ClientConfig_v0_3(Builder builder) {
        this.streaming = builder.streaming == null ? true : builder.streaming;
        this.polling = builder.polling == null ? false : builder.polling;
        this.useClientPreference = builder.useClientPreference == null ? false : builder.useClientPreference;
        this.acceptedOutputModes = builder.acceptedOutputModes;
        this.pushNotificationConfig = builder.pushNotificationConfig;
        this.historyLength = builder.historyLength;
        this.metadata = builder.metadata;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public boolean isPolling() {
        return polling;
    }

    public boolean isUseClientPreference() {
        return useClientPreference;
    }

    public List<String> getAcceptedOutputModes() {
        return acceptedOutputModes;
    }

    public @Nullable PushNotificationConfig_v0_3 getPushNotificationConfig() {
        return pushNotificationConfig;
    }

    public @Nullable Integer getHistoryLength() {
        return historyLength;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private @Nullable Boolean streaming;
        private @Nullable Boolean polling;
        private @Nullable Boolean useClientPreference;
        private List<String> acceptedOutputModes = new ArrayList<>();
        private @Nullable PushNotificationConfig_v0_3 pushNotificationConfig;
        private @Nullable Integer historyLength;
        private Map<String, Object> metadata = new HashMap<>();

        public Builder setStreaming(@Nullable Boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder setPolling(@Nullable Boolean polling) {
            this.polling = polling;
            return this;
        }

        public Builder setUseClientPreference(@Nullable Boolean useClientPreference) {
            this.useClientPreference = useClientPreference;
            return this;
        }

        public Builder setAcceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = new ArrayList<>(acceptedOutputModes);
            return this;
        }

        public Builder setPushNotificationConfig(PushNotificationConfig_v0_3 pushNotificationConfig) {
            this.pushNotificationConfig = pushNotificationConfig;
            return this;
        }

        public Builder setHistoryLength(Integer historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        public Builder setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ClientConfig_v0_3 build() {
            return new ClientConfig_v0_3(this);
        }
    }
}