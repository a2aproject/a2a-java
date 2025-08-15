package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC request for sending a message to an agent in the A2A protocol.
 * This request is used to initiate message communication with an agent, allowing clients
 * to send various types of messages including text, images, and other content.
 *
 * <p>The SendMessageRequest follows the JSON-RPC 2.0 specification and uses the
 * "message/send" method. It encapsulates message parameters and configuration
 * options for delivery and processing.</p>
 *
 * <p>This is a non-streaming request, meaning it expects a single response
 * rather than a stream of responses. For streaming message scenarios,
 * use SendStreamingMessageRequest instead.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * MessageSendParams params = new MessageSendParams.Builder()
 *     .message("Hello, agent!")
 *     .build();
 * SendMessageRequest request = new SendMessageRequest("request-123", params);
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendMessageRequest extends NonStreamingJSONRPCRequest<MessageSendParams> {

    /** The JSON-RPC method name for sending messages */
    public static final String METHOD = "message/send";

    /**
     * Creates a new SendMessageRequest with full parameter specification.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the request identifier (string, number, or null)
     * @param method the JSON-RPC method name (must be "message/send")
     * @param params the message parameters containing the message content and configuration
     * @throws IllegalArgumentException if jsonrpc is null, empty, or not "2.0"
     * @throws IllegalArgumentException if method is not "message/send"
     * @throws IllegalArgumentException if params is null
     * @throws IllegalArgumentException if id is not null, string, or integer
     */
    @JsonCreator
    public SendMessageRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                              @JsonProperty("method") String method, @JsonProperty("params") MessageSendParams params) {
        if (jsonrpc == null || jsonrpc.isEmpty()) {
            throw new IllegalArgumentException("JSON-RPC protocol version cannot be null or empty");
        }
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid SendMessageRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    /**
     * Creates a new SendMessageRequest with simplified parameters.
     * Uses the default JSON-RPC version ("2.0") and method ("message/send").
     * 
     * @param id the request identifier (string, number, or null)
     * @param params the message parameters containing the message content and configuration
     */
    public SendMessageRequest(Object id, MessageSendParams params) {
        this(JSONRPC_VERSION, id, METHOD, params);
    }

    /**
     * Builder class for constructing SendMessageRequest instances.
     * Provides a fluent API for configuring request parameters.
     */
    public static class Builder {
        /** The JSON-RPC protocol version */
        private String jsonrpc;
        
        /** The request identifier */
        private Object id;
        
        /** The JSON-RPC method name */
        private String method;
        
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
         * @param method the method name (should be "message/send")
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
         * Builds and returns a new SendMessageRequest instance.
         * If no ID is specified, generates a random UUID as the request identifier.
         * 
         * @return a new SendMessageRequest with the configured parameters
         * @throws IllegalArgumentException if required parameters are invalid
         */
        public SendMessageRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SendMessageRequest(jsonrpc, id, method, params);
        }
    }
}
