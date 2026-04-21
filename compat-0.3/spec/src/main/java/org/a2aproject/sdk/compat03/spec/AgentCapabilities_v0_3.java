package org.a2aproject.sdk.compat03.spec;

import java.util.List;

/**
 * Defines optional capabilities supported by an agent.
 */
public record AgentCapabilities_v0_3(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory,
                                     List<AgentExtension_v0_3> extensions) {

    public static class Builder {

        private boolean streaming;
        private boolean pushNotifications;
        private boolean stateTransitionHistory;
        private List<AgentExtension_v0_3> extensions;

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

        public Builder extensions(List<AgentExtension_v0_3> extensions) {
            this.extensions = extensions;
            return this;
        }

        public AgentCapabilities_v0_3 build() {
            return new AgentCapabilities_v0_3(streaming, pushNotifications, stateTransitionHistory, extensions);
        }
    }
}
