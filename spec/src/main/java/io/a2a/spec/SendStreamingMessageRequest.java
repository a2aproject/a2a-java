package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

import java.util.UUID;

/**
 * Represents a JSON-RPC request for sending a streaming message to an agent in the A2A protocol.
 * This request is used to initiate streaming message communication with an agent, allowing clients
 * to send messages that expect a continuous stream of responses rather than a single response.
 *
 * <p>The SendStreamingMessageRequest follows the JSON-RPC 2.0 specification and uses the
 * "message/stream" method. It encapsulates message parameters and configuration options
 * for streaming delivery and processing.</p>
 *
 * <p>Unlike SendMessageRequest which expects a single response, this streaming variant
 * allows the agent to send multiple responses over time, making it suitable for:
 * <ul>
 *   <li>Long-running tasks that provide progress updates</li>
 *   <li>Interactive conversations with real-time responses</li>
 *   <li>Tasks that generate incremental results</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * MessageSendParams params = new MessageSendParams.Builder()
 *     .message("Generate a long report")
 *     .build();
 * SendStreamingMessageRequest request = new SendStreamingMessageRequest("stream-123", params);
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendStreamingMessageRequest extends StreamingJSONRPCRequest<MessageSendParams> {

    /** The JSON-RPC method name for streaming messages */
    public static final String METHOD = "message/stream";

    /**
     * Creates a new SendStreamingMessageRequest with full parameter specification.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the request identifier (string, number, or null)
     * @param method the JSON-RPC method name (must be "message/stream")
     * @param params the message parameters containing the message content and configuration
     * @throws IllegalArgumentException if jsonrpc is not "2.0"
     * @throws IllegalArgumentException if method is not "message/stream"
     * @throws IllegalArgumentException if params is null
     * @throws IllegalArgumentException if id is not null, string, or integer
     */
    @JsonCreator
    public SendStreamingMessageRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                       @JsonProperty("method") String method, @JsonProperty("params") MessageSendParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid SendStreamingMessageRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    /**
     * Creates a new SendStreamingMessageRequest with simplified parameters.
     * Uses the default JSON-RPC version ("2.0") and method ("message/stream").
     * 
     * @param id the request identifier (string, number, or null)
     * @param params the message parameters containing the message content and configuration
     */
    public SendStreamingMessageRequest(Object id,  MessageSendParams params) {
        this(null, id, METHOD, params);
    }

    /**
     * Builder class for constructing SendStreamingMessageRequest instances.
     * Provides a fluent API for configuring streaming request parameters.
     */
    public static class Builder {
            /** The JSON-RPC protocol version */
            private String jsonrpc;
            
            /** The request identifier */
            private Object id;
            
            /** The JSON-RPC method name (defaults to "message/stream") */
            private String method = METHOD;
            
            /** The message parameters */
            private MessageSendParams params;

            /**
             * Sets the JSON-RPC protocol version.
             * 
             * @param jsonrpc the JSON-RPC version (should be "2.0")
             * @return this builder instance for method chaining
             */
            public Builder jsonrpc(String jsonrpc) {
                this.jsonrpc = jsonrpc;
                return this;
            }

            /**
             * Sets the request identifier.
             * 
             * @param id the request identifier (string, number, or null)
             * @return this builder instance for method chaining
             */
            public Builder id(Object id) {
                this.id = id;
                return this;
            }

            /**
             * Sets the JSON-RPC method name.
             * 
             * @param method the method name (should be "message/stream")
             * @return this builder instance for method chaining
             */
            public Builder method(String method) {
                this.method = method;
                return this;
            }

            /**
             * Sets the message parameters.
             * 
             * @param params the message parameters containing content and configuration
             * @return this builder instance for method chaining
             */
            public Builder params(MessageSendParams params) {
                this.params = params;
                return this;
            }

            /**
             * Builds and returns a new SendStreamingMessageRequest instance.
             * If no ID is specified, generates a random UUID as the request identifier.
             * 
             * @return a new SendStreamingMessageRequest with the configured parameters
             * @throws IllegalArgumentException if required parameters are invalid
             */
            public SendStreamingMessageRequest build() {
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
                return new SendStreamingMessageRequest(jsonrpc, id, method, params);
            }
        }
    }
