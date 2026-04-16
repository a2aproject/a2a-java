package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

import java.util.UUID;

import org.a2aproject.sdk.util.Assert;

/**
 * Used to send a message request.
 */
public final class SendMessageRequest_v0_3 extends NonStreamingJSONRPCRequest_v0_3<MessageSendParams_v0_3> {

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
    public SendMessageRequest_v0_3(String jsonrpc, Object id, String method, MessageSendParams_v0_3 params) {
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

    public SendMessageRequest_v0_3(Object id, MessageSendParams_v0_3 params) {
        this(JSONRPC_VERSION, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private MessageSendParams_v0_3 params;

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

        public Builder params(MessageSendParams_v0_3 params) {
            this.params = params;
            return this;
        }

        public SendMessageRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SendMessageRequest_v0_3(jsonrpc, id, method, params);
        }
    }
}
