package io.a2a.spec;

import java.util.List;

/**
 * Defines optional capabilities supported by an agent in the A2A Protocol.
 * <p>
 * AgentCapabilities advertises which advanced features an agent supports beyond the
 * basic request-response pattern. Clients can inspect these capabilities to determine
 * how to interact with the agent and what features are available.
 * <p>
 * <b>Core Capabilities:</b>
 * <ul>
 *   <li><b>streaming:</b> Agent can produce incremental results via streaming responses,
 *       allowing clients to receive partial artifacts as they are generated rather than
 *       waiting for task completion</li>
 *   <li><b>pushNotifications:</b> Agent can send proactive notifications to clients
 *       when task state changes, eliminating the need for polling</li>
 *   <li><b>stateTransitionHistory:</b> Agent maintains and provides a complete history
 *       of all state transitions for tasks, useful for debugging and auditing</li>
 * </ul>
 * <p>
 * Capabilities are declared in the {@link AgentCard} and are immutable for the lifetime
 * of the agent instance. This class uses the Builder pattern for construction.
 *
 * @param streaming whether the agent supports streaming responses with incremental artifacts
 * @param pushNotifications whether the agent supports push notifications for state changes
 * @param stateTransitionHistory whether the agent maintains state transition history
 * @param extensions list of custom extensions supported by the agent (optional)
 * @see AgentCard
 * @see AgentExtension
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record AgentCapabilities(boolean streaming, boolean pushNotifications, boolean stateTransitionHistory,
                                List<AgentExtension> extensions) {

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }
    /**
     * Builder for constructing immutable {@link AgentCapabilities} instances.
     * <p>
     * The Builder pattern provides a fluent API for setting capability flags.
     * All capabilities default to false if not explicitly set.
     * <p>
     * Example usage:
     * <pre>{@code
     * AgentCapabilities capabilities = AgentCapabilities.builder()
     *     .streaming(true)
     *     .pushNotifications(false)
     *     .stateTransitionHistory(false)
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private boolean streaming;
        private boolean pushNotifications;
        private boolean stateTransitionHistory;
        private List<AgentExtension> extensions;

        /**
         * Creates a new Builder with all capabilities set to false by default.
         */
        private Builder() {
        }

        /**
         * Sets whether the agent supports streaming responses.
         * <p>
         * When enabled, clients can subscribe to task updates and receive
         * incremental artifacts as the agent produces them.
         *
         * @param streaming true if streaming is supported, false otherwise
         * @return this builder for method chaining
         */
        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        /**
         * Sets whether the agent supports push notifications.
         * <p>
         * When enabled, the agent can proactively notify clients of task
         * state changes, eliminating the need for polling.
         *
         * @param pushNotifications true if push notifications are supported, false otherwise
         * @return this builder for method chaining
         */
        public Builder pushNotifications(boolean pushNotifications) {
            this.pushNotifications = pushNotifications;
            return this;
        }

        /**
         * Sets whether the agent maintains state transition history.
         * <p>
         * When enabled, the agent tracks and provides a complete history
         * of all state transitions for each task.
         *
         * @param stateTransitionHistory true if state history is maintained, false otherwise
         * @return this builder for method chaining
         */
        public Builder stateTransitionHistory(boolean stateTransitionHistory) {
            this.stateTransitionHistory = stateTransitionHistory;
            return this;
        }

        /**
         * Sets the list of custom extensions supported by the agent.
         * <p>
         * Extensions allow agents to advertise proprietary or experimental
         * capabilities beyond the core A2A Protocol.
         *
         * @param extensions list of agent extensions (optional)
         * @return this builder for method chaining
         * @see AgentExtension
         */
        public Builder extensions(List<AgentExtension> extensions) {
            this.extensions = extensions;
            return this;
        }

        /**
         * Builds an immutable {@link AgentCapabilities} from the current builder state.
         *
         * @return a new AgentCapabilities instance
         */
        public AgentCapabilities build() {
            return new AgentCapabilities(streaming, pushNotifications, stateTransitionHistory, extensions);
        }
    }
}
