package org.a2aproject.sdk.jsonrpc.common.wrappers;

import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;

import java.util.UUID;

import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Task;

/**
 * JSON-RPC request for the {@code SendMessage} method in the A2A Protocol.
 * <p>
 * This request initiates a new task or continues an existing task by sending a message
 * to an agent. The request returns a single response containing the final {@link Task}
 * state once processing completes (blocking/non-streaming mode).
 * <p>
 * The {@code SendMessage} method is used for:
 * <ul>
 *   <li>Creating new tasks with an initial user message</li>
 *   <li>Continuing existing tasks with additional messages</li>
 *   <li>Restarting tasks with new context</li>
 * </ul>
 * <p>
 * For real-time event streaming during task execution, use {@link SendStreamingMessageRequest}
 * with the {@code SendStreamingMessage} method instead.
 * <p>
 * This class includes a fluent {@link Builder} for convenient request construction. The method
 * name is fixed as "SendMessage" and is set automatically by the constructor.
 *
 * @see SendStreamingMessageRequest
 * @see MessageSendParams
 * @see Task
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class SendMessageRequest extends NonStreamingJSONRPCRequest<MessageSendParams> {

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
        super(jsonrpc, SEND_MESSAGE_METHOD, id, params);
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
     * The method name is automatically set to "SendMessage" and cannot be changed.
     * If no ID is provided, a UUID will be auto-generated when {@link #build()} is called.
     * <p>
     * Example usage:
     * <pre>{@code
     * SendMessageRequest request = SendMessageRequest.builder()
     *     .params(new MessageSendParams(message, taskId, taskPushNotificationConfig))
     *     .build();
     * }</pre>
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

        /**
         * Sets the JSON-RPC version.
         *
         * @param jsonrpc the JSON-RPC version (typically "2.0")
         * @return this builder for method chaining
         */
        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request correlation ID.
         *
         * @param id the request correlation identifier
         * @return this builder for method chaining
         */
        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the message send parameters (required).
         *
         * @param params the message send parameters
         * @return this builder for method chaining
         */
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
