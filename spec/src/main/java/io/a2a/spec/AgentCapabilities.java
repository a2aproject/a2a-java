package io.a2a.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the optional A2A protocol features and capabilities supported by an agent.
 * These capabilities define what advanced features the agent can handle beyond basic messaging.
 *
 * @param streaming whether the agent supports streaming responses for real-time communication
 * @param pushNotifications whether the agent can send push notifications to clients
 * @param stateTransitionHistory whether the agent maintains and provides access to state transition history
 * @param extensions list of additional extensions or custom capabilities supported by the agent
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentCapabilities(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory,
                                List<AgentExtension> extensions) {

    /**
     * Builder class for constructing {@link AgentCapabilities} instances.
     */
    public static class Builder {

        private boolean streaming;
        private boolean pushNotifications;
        private boolean stateTransitionHistory;
        private List<AgentExtension> extensions;

        /**
         * Sets whether the agent supports streaming responses.
         *
         * @param streaming {@code true} if the agent supports streaming responses
         * @return this builder instance
         */
        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        /**
         * Sets whether the agent can send push notifications.
         *
         * @param pushNotifications {@code true} if the agent supports push notifications
         * @return this builder instance
         */
        public Builder pushNotifications(boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
            return this;
        }

        /**
         * Sets whether the agent maintains state transition history.
         *
         * @param stateTransitionHistory {@code true} if the agent provides state transition history
         * @return this builder instance
         */
        public Builder stateTransitionHistory(boolean stateTransitionHistory) {
            this.stateTransitionHistory = stateTransitionHistory;
            return this;
        }

        /**
         * Sets the list of additional extensions supported by the agent.
         *
         * @param extensions the list of agent extensions
         * @return this builder instance
         */
        public Builder extensions(List<AgentExtension> extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Builds and returns a new {@link AgentCapabilities} instance with the configured properties.
         *
         * @return a new AgentCapabilities instance
         */
        public AgentCapabilities build() {
            return new AgentCapabilities(streaming, pushNotifications, stateTransitionHistory, extensions);
        }
    }
}
