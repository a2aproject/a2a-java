package io.a2a.spec;

import java.util.UUID;

/**
 * JSON-RPC request for the {@code message/send} method in the A2A Protocol.
 * <p>
 * This request initiates a new task or continues an existing task by sending a message
 * to an agent. The request returns a single response containing the final {@link Task}
 * state once processing completes.
 * <p>
 * The {@code message/send} method is used for:
 * <ul>
 *   <li>Creating new tasks with an initial user message</li>
 *   <li>Continuing existing tasks with additional messages</li>
 *   <li>Restarting tasks with new context</li>
 * </ul>
 * <p>
 * For real-time event streaming during task execution, use {@link SendStreamingMessageRequest}
 * with the {@code message/stream} method instead.
 * <p>
 * This class includes a fluent {@link Builder} for convenient request construction.
 *
 * @see SendStreamingMessageRequest
 * @see MessageSendParams
 * @see Task
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class SendMessageRequest extends NonStreamingJSONRPCRequest<MessageSendParams> {

    /**
     * The JSON-RPC method name for sending a message: "SendMessage".
     */
    public static final String METHOD = "SendMessage";

    /**
     * Constructs a SendMessageRequest with the specified JSON-RPC fields.
     * <p>
     * This constructor is used for JSON deserialization and validates
     * that the method name is exactly "SendMessage".
     *
     * @param jsonrpc the JSON-RPC version (must be "2.0")
     * @param id      the request correlation identifier (String, Integer, or null)
     * @param params  the message send parameters (required)
     * @throws IllegalArgumentException if validation fails
     */
    public SendMessageRequest(String jsonrpc, Object id, MessageSendParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**
     * Constructs a SendMessageRequest with default JSON-RPC version.
     *
     * @param id the request correlation identifier
     * @param params the message send parameters (required)
     */
    public SendMessageRequest(Object id, MessageSendParams params) {
        this(JSONRPC_VERSION, id, params);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SendMessageRequest} instances with a fluent API.
     * <p>
     * If no ID is provided, a UUID will be auto-generated when {@link #build()} is called.
     */
    public static class Builder {
        private String jsonrpc;
        private Object id;
        private MessageSendParams params;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /** Sets the JSON-RPC version. */
        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /** Sets the request correlation ID. */
        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the method name.
         *
         * @deprecated
         * */
        public Builder method(String method) {
            return this;
        }

        /** Sets the message send parameters (required). */
        public Builder params(MessageSendParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds the SendMessageRequest.
         * Auto-generates a UUID for the ID if not set.
         *
         * @return the constructed request
         */
        public SendMessageRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SendMessageRequest(jsonrpc, id, params);
        }
    }
}
