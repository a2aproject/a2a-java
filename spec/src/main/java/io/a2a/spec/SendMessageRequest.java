package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import io.a2a.util.Assert;

/**
 * Used to send a message request.
 */
public final class SendMessageRequest extends NonStreamingJSONRPCRequest<MessageSendParams> {

    public static final String METHOD = "message/send";

    /**
     * Constructs a SendMessageRequest with the specified JSON-RPC fields.
     * <p>
     * This constructor is used for JSON deserialization and validates
     * that the method name is exactly "SendMessage".
     *
     * @param jsonrpc the JSON-RPC version (must be "2.0")
     * @param id the request correlation identifier (String, Integer, or null)
     * @param method the method name (must be {@value #METHOD})
     * @param params the message send parameters (required)
     * @throws IllegalArgumentException if validation fails
     */
    public SendMessageRequest(String jsonrpc, Object id, String method, MessageSendParams params) {
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

    public SendMessageRequest(Object id, MessageSendParams params) {
        this(JSONRPC_VERSION, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private MessageSendParams params;

        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(MessageSendParams params) {
            this.params = params;
            return this;
        }

        public SendMessageRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SendMessageRequest(jsonrpc, id, method, params);
        }
    }
}
