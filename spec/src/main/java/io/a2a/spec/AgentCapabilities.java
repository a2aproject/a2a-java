package io.a2a.spec;

import java.util.List;

/**
 * Defines optional capabilities supported by an agent.
 *
 * @param streaming whether the agent supports streaming responses
 * @param pushNotifications whether the agent supports push notifications
 * @param stateTransitionHistory whether the agent supports state transition history
 * @param extensions optional list of protocol extensions supported by the agent
 */
public record AgentCapabilities(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory,
                                List<AgentExtension> extensions) {

    public static class Builder {

        private boolean streaming;
        private boolean pushNotifications;
        private boolean stateTransitionHistory;
        private List<AgentExtension> extensions;

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder pushNotifications(boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
            return this;
        }

        public Builder stateTransitionHistory(boolean stateTransitionHistory) {
            this.stateTransitionHistory = stateTransitionHistory;
            return this;
        }

        public Builder extensions(List<AgentExtension> extensions) {
            this.extensions = extensions;
            return this;
        }

        public AgentCapabilities build() {
            return new AgentCapabilities(streaming, pushNotifications, stateTransitionHistory, extensions);
        }
    }
}
