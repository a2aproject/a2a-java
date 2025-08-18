package io.a2a.client.config;

import java.util.List;
import java.util.Map;

import io.a2a.spec.PushNotificationConfig;

/**
 * Configuration for the A2A client factory.
 */
public class ClientConfig {

    private final Boolean streaming;
    private final Boolean polling;
    private final List<ClientTransportConfig> clientTransportConfigs;
    private final List<String> supportedTransports;
    private final Boolean useClientPreference;
    private final List<String> acceptedOutputModes;
    private final PushNotificationConfig pushNotificationConfig;
    private final Integer historyLength;
    private final Map<String, Object> metadata;

    public ClientConfig(Boolean streaming, Boolean polling, List<ClientTransportConfig> clientTransportConfigs,
                        List<String> supportedTransports, Boolean useClientPreference,
                        List<String> acceptedOutputModes, PushNotificationConfig pushNotificationConfig,
                        Integer historyLength, Map<String, Object> metadata) {
        this.streaming = streaming == null ? true : streaming;
        this.polling = polling == null ? false : polling;
        this.clientTransportConfigs = clientTransportConfigs;
        this.supportedTransports = supportedTransports;
        this.useClientPreference = useClientPreference == null ? false : useClientPreference;
        this.acceptedOutputModes = acceptedOutputModes;
        this.pushNotificationConfig = pushNotificationConfig;
        this.historyLength = historyLength;
        this.metadata = metadata;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public boolean isPolling() {
        return polling;
    }

    public List<ClientTransportConfig> getClientTransportConfigs() {
        return clientTransportConfigs;
    }

    public List<String> getSupportedTransports() {
        return supportedTransports;
    }

    public boolean isUseClientPreference() {
        return useClientPreference;
    }

    public List<String> getAcceptedOutputModes() {
        return acceptedOutputModes;
    }

    public PushNotificationConfig getPushNotificationConfig() {
        return pushNotificationConfig;
    }

    public Integer getHistoryLength() {
        return historyLength;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static class Builder {
        private Boolean streaming;
        private Boolean polling;
        private List<ClientTransportConfig> clientTransportConfigs;
        private List<String> supportedTransports;
        private Boolean useClientPreference;
        private List<String> acceptedOutputModes;
        private PushNotificationConfig pushNotificationConfig;
        private Integer historyLength;
        private Map<String, Object> metadata;

        public Builder setStreaming(Boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder setPolling(Boolean polling) {
            this.polling = polling;
            return this;
        }

        public Builder setClientTransportConfigs(List<ClientTransportConfig> clientTransportConfigs) {
            this.clientTransportConfigs = clientTransportConfigs;
            return this;
        }

        public Builder setSupportedTransports(List<String> supportedTransports) {
            this.supportedTransports = supportedTransports;
            return this;
        }

        public Builder setUseClientPreference(Boolean useClientPreference) {
            this.useClientPreference = useClientPreference;
            return this;
        }

        public Builder setAcceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = acceptedOutputModes;
            return this;
        }

        public Builder setPushNotificationConfig(PushNotificationConfig pushNotificationConfig) {
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

        public ClientConfig build() {
            return new ClientConfig(streaming, polling, clientTransportConfigs,
                    supportedTransports, useClientPreference, acceptedOutputModes,
                    pushNotificationConfig, historyLength, metadata);
        }
    }
}