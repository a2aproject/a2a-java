package org.a2aproject.sdk.client.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for controlling A2A client behavior and communication preferences.
 * <p>
 * ClientConfig defines how the client communicates with agents, including streaming mode,
 * transport preference, output modes, and request metadata. The configuration is immutable
 * and constructed using the {@link Builder} pattern.
 * <p>
 * <b>Key configuration options:</b>
 * <ul>
 *   <li><b>Streaming:</b> Enable/disable real-time event streaming (default: true)</li>
 *   <li><b>Polling:</b> Use polling instead of blocking for updates (default: false)</li>
 *   <li><b>Transport preference:</b> Client vs server transport priority (default: server preference)</li>
 *   <li><b>Output modes:</b> Acceptable content types (text, audio, image, etc.)</li>
 *   <li><b>History length:</b> Number of previous messages to include as context</li>
 *   <li><b>Push notifications:</b> Default webhook configuration for task updates</li>
 *   <li><b>Metadata:</b> Custom metadata attached to all requests</li>
 * </ul>
 * <p>
 * <b>Streaming mode:</b> Controls whether the client uses streaming or blocking communication.
 * Streaming mode requires both the client configuration AND the agent's capabilities to support it:
 * <pre>{@code
 * // Enable streaming (if agent also supports it)
 * ClientConfig config = new ClientConfig.Builder()
 *     .setStreaming(true)
 *     .build();
 *
 * // Actual mode = config.streaming && agentCard.capabilities().streaming()
 * }</pre>
 * When streaming is enabled and supported, the client receives events asynchronously as the
 * agent processes the request. When disabled, the client blocks until the task completes.
 * <p>
 * <b>Transport preference:</b> Controls which transport protocol is selected when multiple
 * options are available:
 * <pre>{@code
 * // Default: Use server's preferred transport (first in AgentCard.supportedInterfaces)
 * ClientConfig serverPref = new ClientConfig.Builder()
 *     .setUseClientPreference(false)
 *     .build();
 *
 * // Use client's preferred transport (order of withTransport() calls)
 * ClientConfig clientPref = new ClientConfig.Builder()
 *     .setUseClientPreference(true)
 *     .build();
 *
 * Client client = Client.builder(card)
 *     .withTransport(GrpcTransport.class, grpcConfig)      // Client preference 1
 *     .withTransport(JSONRPCTransport.class, jsonConfig)   // Client preference 2
 *     .clientConfig(clientPref)
 *     .build();
 * // With useClientPreference=true, tries gRPC first, then JSON-RPC
 * // With useClientPreference=false, uses server's order from AgentCard
 * }</pre>
 * <p>
 * <b>Output modes:</b> Specify which content types the client can handle:
 * <pre>{@code
 * ClientConfig config = new ClientConfig.Builder()
 *     .setAcceptedOutputModes(List.of("text", "image", "audio"))
 *     .build();
 * // Agent will only return text, image, or audio content
 * }</pre>
 * <p>
 * <b>Conversation history:</b> Request previous messages as context:
 * <pre>{@code
 * ClientConfig config = new ClientConfig.Builder()
 *     .setHistoryLength(10)  // Include last 10 messages
 *     .build();
 * }</pre>
 * This is useful for maintaining conversation context across multiple requests in the same session.
 * <p>
 * <b>Push notifications:</b> Configure default webhook for all task updates:
 * <pre>{@code
 * TaskPushNotificationConfig pushConfig = TaskPushNotificationConfig.builder()
 *     .id("config-1")
 *     .url("https://my-app.com/webhooks/tasks")
 *     .authentication(new AuthenticationInfo("bearer", "my-token"))
 *     .build();
 * ClientConfig config = new ClientConfig.Builder()
 *     .setTaskPushNotificationConfig(pushConfig)
 *     .build();
 * // All sendMessage() calls will use this webhook config
 * }</pre>
 * <p>
 * <b>Custom metadata:</b> Attach metadata to all requests:
 * <pre>{@code
 * Map<String, Object> metadata = Map.of(
 *     "userId", "user-123",
 *     "sessionId", "session-456",
 *     "clientVersion", "1.0.0"
 * );
 * ClientConfig config = new ClientConfig.Builder()
 *     .setMetadata(metadata)
 *     .build();
 * // Metadata is included in every message sent
 * }</pre>
 * <p>
 * <b>Complete example:</b>
 * <pre>{@code
 * ClientConfig config = new ClientConfig.Builder()
 *     .setStreaming(true)                         // Enable streaming
 *     .setUseClientPreference(true)               // Use client transport order
 *     .setAcceptedOutputModes(List.of("text"))    // Text responses only
 *     .setHistoryLength(5)                        // Last 5 messages as context
 *     .setMetadata(Map.of("userId", "user-123"))  // Custom metadata
 *     .build();
 *
 * Client client = Client.builder(agentCard)
 *     .clientConfig(config)
 *     .withTransport(JSONRPCTransport.class, transportConfig)
 *     .build();
 * }</pre>
 * <p>
 * <b>Default values:</b>
 * <ul>
 *   <li>streaming: {@code true}</li>
 *   <li>polling: {@code false}</li>
 *   <li>useClientPreference: {@code false} (server preference)</li>
 *   <li>acceptedOutputModes: empty list (accept all)</li>
 *   <li>historyLength: {@code null} (no history)</li>
 *   <li>taskPushNotificationConfig: {@code null} (no push notifications)</li>
 *   <li>metadata: empty map</li>
 * </ul>
 * <p>
 * <b>Thread safety:</b> ClientConfig is immutable and thread-safe. Multiple clients can
 * share the same configuration instance.
 *
 * @see org.a2aproject.sdk.client.Client
 * @see org.a2aproject.sdk.client.ClientBuilder
 * @see TaskPushNotificationConfig
 */
public class ClientConfig {

    private final Boolean streaming;
    private final Boolean polling;
    private final Boolean useClientPreference;
    private final List<String> acceptedOutputModes;
    private final @Nullable TaskPushNotificationConfig taskPushNotificationConfig;
    private final @Nullable Integer historyLength;
    private final Map<String, Object> metadata;

    private ClientConfig(Builder builder) {
        this.streaming = builder.streaming == null ? true : builder.streaming;
        this.polling = builder.polling == null ? false : builder.polling;
        this.useClientPreference = builder.useClientPreference == null ? false : builder.useClientPreference;
        this.acceptedOutputModes = builder.acceptedOutputModes;
        this.taskPushNotificationConfig = builder.taskPushNotificationConfig;
        this.historyLength = builder.historyLength;
        this.metadata = builder.metadata;
    }

    /**
     * Check if streaming mode is enabled.
     * <p>
     * Note: Actual streaming requires both this configuration AND agent support
     * ({@link org.a2aproject.sdk.spec.AgentCapabilities#streaming()}).
     *
     * @return {@code true} if streaming is enabled (default)
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Check if polling mode is enabled for task updates.
     * <p>
     * When polling is enabled, the client can poll for task status updates instead of
     * blocking or streaming. This is useful for asynchronous workflows where the client
     * doesn't need immediate results.
     *
     * @return {@code true} if polling is enabled, {@code false} by default
     */
    public boolean isPolling() {
        return polling;
    }

    /**
     * Check if client transport preference is enabled.
     * <p>
     * When {@code true}, the client iterates through its configured transports (in the order
     * they were added via {@link org.a2aproject.sdk.client.ClientBuilder#withTransport}) and selects the first one
     * the agent supports.
     * <p>
     * When {@code false} (default), the agent's preferred transport is used (first entry
     * in {@link org.a2aproject.sdk.spec.AgentCard#supportedInterfaces()}).
     *
     * @return {@code true} if using client preference, {@code false} for server preference (default)
     */
    public boolean isUseClientPreference() {
        return useClientPreference;
    }

    /**
     * Get the list of accepted output modes.
     * <p>
     * This list specifies which content types the client can handle (e.g., "text", "audio",
     * "image", "video"). An empty list means all modes are accepted.
     * <p>
     * The agent will only return content in the specified modes. For example, if only "text"
     * is specified, the agent won't return images or audio.
     *
     * @return the list of accepted output modes (never null, but may be empty)
     */
    public List<String> getAcceptedOutputModes() {
        return acceptedOutputModes;
    }

    /**
     * Get the default push notification configuration.
     * <p>
     * If set, this webhook configuration will be used for all sendMessage
     * calls unless overridden with a different configuration.
     *
     * @return the push notification config, or {@code null} if not configured
     * @see org.a2aproject.sdk.client.Client#sendMessage(org.a2aproject.sdk.spec.Message, org.a2aproject.sdk.spec.TaskPushNotificationConfig, java.util.Map, org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext)
     */
    public @Nullable TaskPushNotificationConfig getTaskPushNotificationConfig() {
        return taskPushNotificationConfig;
    }

    /**
     * Get the conversation history length.
     * <p>
     * This value specifies how many previous messages should be included as context
     * when sending a new message. For example, a value of 10 means the agent receives
     * the last 10 messages in the conversation for context.
     *
     * @return the history length, or {@code null} if not configured (no history)
     */
    public @Nullable Integer getHistoryLength() {
        return historyLength;
    }

    /**
     * Get the custom metadata attached to all requests.
     * <p>
     * This metadata is included in every message sent by the client. It can contain
     * user IDs, session identifiers, client version, or any other custom data.
     *
     * @return the metadata map (never null, but may be empty)
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Create a new builder for constructing ClientConfig instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating {@link ClientConfig} instances.
     * <p>
     * All configuration options have sensible defaults and are optional. Use this builder
     * to override specific settings as needed.
     * <p>
     * Example:
     * <pre>{@code
     * ClientConfig config = new ClientConfig.Builder()
     *     .setStreaming(true)
     *     .setHistoryLength(10)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private @Nullable Boolean streaming;
        private @Nullable Boolean polling;
        private @Nullable Boolean useClientPreference;
        private List<String> acceptedOutputModes = new ArrayList<>();
        private @Nullable TaskPushNotificationConfig taskPushNotificationConfig;
        private @Nullable Integer historyLength;
        private Map<String, Object> metadata = new HashMap<>();

        /**
         * Enable or disable streaming mode.
         * <p>
         * When enabled, the client will use streaming communication if the agent also
         * supports it. When disabled, the client uses blocking request-response mode.
         *
         * @param streaming {@code true} to enable streaming (default), {@code false} to disable
         * @return this builder for method chaining
         */
        public Builder setStreaming(@Nullable Boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        /**
         * Enable or disable polling mode for task updates.
         * <p>
         * When enabled, the client can poll for task status instead of blocking or streaming.
         * Useful for asynchronous workflows.
         *
         * @param polling {@code true} to enable polling, {@code false} otherwise (default)
         * @return this builder for method chaining
         */
        public Builder setPolling(@Nullable Boolean polling) {
            this.polling = polling;
            return this;
        }

        /**
         * Set whether to use client or server transport preference.
         * <p>
         * When {@code true}, the client's transport order (from {@link org.a2aproject.sdk.client.ClientBuilder#withTransport}
         * calls) takes priority. When {@code false} (default), the server's preferred transport
         * (first in {@link org.a2aproject.sdk.spec.AgentCard#supportedInterfaces()}) is used.
         *
         * @param useClientPreference {@code true} for client preference, {@code false} for server preference (default)
         * @return this builder for method chaining
         */
        public Builder setUseClientPreference(@Nullable Boolean useClientPreference) {
            this.useClientPreference = useClientPreference;
            return this;
        }

        /**
         * Set the accepted output modes.
         * <p>
         * Specify which content types the client can handle (e.g., "text", "audio", "image").
         * An empty list (default) means all modes are accepted.
         * <p>
         * The provided list is copied, so subsequent modifications won't affect this configuration.
         *
         * @param acceptedOutputModes the list of accepted output modes
         * @return this builder for method chaining
         */
        public Builder setAcceptedOutputModes(List<String> acceptedOutputModes) {
            this.acceptedOutputModes = new ArrayList<>(acceptedOutputModes);
            return this;
        }

        /**
         * Set the default push notification configuration.
         * <p>
         * This webhook configuration will be used for all sendMessage calls
         * unless overridden. The agent will POST task update events to the specified URL.
         *
         * @param taskPushNotificationConfig the push notification configuration
         * @return this builder for method chaining
         * @see org.a2aproject.sdk.client.Client#sendMessage(org.a2aproject.sdk.spec.Message, org.a2aproject.sdk.spec.TaskPushNotificationConfig, java.util.Map, org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext)
         */
        public Builder setTaskPushNotificationConfig(TaskPushNotificationConfig taskPushNotificationConfig) {
            this.taskPushNotificationConfig = taskPushNotificationConfig;
            return this;
        }

        /**
         * Set the conversation history length.
         * <p>
         * Specify how many previous messages should be included as context when sending
         * a new message. For example, 10 means the last 10 messages are sent to the agent
         * for context.
         *
         * @param historyLength the number of previous messages to include (must be positive)
         * @return this builder for method chaining
         */
        public Builder setHistoryLength(Integer historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        /**
         * Set custom metadata to be included in all requests.
         * <p>
         * This metadata is attached to every message sent by the client. Useful for
         * tracking user IDs, session identifiers, client version, etc.
         * <p>
         * The provided map is copied, so subsequent modifications won't affect this configuration.
         *
         * @param metadata the custom metadata map
         * @return this builder for method chaining
         */
        public Builder setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Build the ClientConfig with the configured settings.
         * <p>
         * Any unset options will use their default values:
         * <ul>
         *   <li>streaming: {@code true}</li>
         *   <li>polling: {@code false}</li>
         *   <li>useClientPreference: {@code false}</li>
         *   <li>acceptedOutputModes: empty list</li>
         *   <li>taskPushNotificationConfig: {@code null}</li>
         *   <li>historyLength: {@code null}</li>
         *   <li>metadata: empty map</li>
         * </ul>
         *
         * @return the configured ClientConfig instance
         */
        public ClientConfig build() {
            return new ClientConfig(this);
        }
    }
}