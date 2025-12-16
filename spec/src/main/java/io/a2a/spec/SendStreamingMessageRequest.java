package io.a2a.spec;

import java.util.UUID;

/**
 * JSON-RPC request to initiate a new task with streaming event delivery.
 * <p>
 * This request starts agent processing for a message and immediately returns a stream of events
 * representing the agent's progress and responses. Unlike {@link SendMessageRequest}, this enables
 * real-time progress updates as the agent processes the request.
 * <p>
 * The streaming response provides {@link StreamingEventKind} events such as task updates,
 * artifact additions, and status changes as they occur, rather than waiting for final completion.
 * <p>
 * This class implements the JSON-RPC {@code message/stream} method as specified in the A2A Protocol.
 *
 * @see SendMessageRequest for non-streaming message delivery
 * @see MessageSendParams for parameter structure
 * @see StreamingEventKind for event types in streaming responses
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class SendStreamingMessageRequest extends StreamingJSONRPCRequest<MessageSendParams> {

    /** The JSON-RPC method name. */


    public static final String METHOD = "SendStreamingMessage";

    /**


     * Constructs request with all parameters.


     *


     * @param jsonrpc the JSON-RPC version


     * @param id the request ID


     * @param params the request parameters


     */


    public SendStreamingMessageRequest(String jsonrpc, Object id, MessageSendParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**
     * Constructs request with ID and parameters.
     *
     * @param id the request ID
     * @param params the request parameters
     */
    public SendStreamingMessageRequest(Object id,  MessageSendParams params) {
        this(null, id, params);
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
     * Builder for constructing {@link SendStreamingMessageRequest} instances.
     * <p>
     * Provides a fluent API for building streaming message requests with optional customization
     * of JSON-RPC protocol fields.
     */
    /**

     * Builder for constructing instances.

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
             * Sets the JSON-RPC protocol version.
             *
             * @param jsonrpc the JSON-RPC version (defaults to "2.0")
             * @return this builder
             */
            /**
             * Sets the jsonrpc.
             *
             * @param jsonrpc the jsonrpc
             * @return this builder for method chaining
             */
            public Builder jsonrpc(String jsonrpc) {
                this.jsonrpc = jsonrpc;
                return this;
            }

            /**
             * Sets the JSON-RPC request ID.
             *
             * @param id the request identifier (auto-generated UUID if null)
             * @return this builder
             */
            /**
             * Sets the id.
             *
             * @param id the id
             * @return this builder for method chaining
             */
            public Builder id(Object id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the message send parameters.
             *
             * @param params the parameters containing the message and configuration
             * @return this builder
             */
            /**
             * Sets the params.
             *
             * @param params the params
             * @return this builder for method chaining
             */
            public Builder params(MessageSendParams params) {
                this.params = params;
                return this;
            }

            /**
             * Builds the {@link SendStreamingMessageRequest}.
             *
             * @return a new streaming message request instance
             */
            /**

             * Builds the instance.

             *

             * @return a new instance

             */

            public SendStreamingMessageRequest build() {
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
                return new SendStreamingMessageRequest(jsonrpc, id, params);
            }
        }
    }
