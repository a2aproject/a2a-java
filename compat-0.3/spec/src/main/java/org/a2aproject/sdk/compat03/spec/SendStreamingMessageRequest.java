package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils.defaultIfNull;

import org.a2aproject.sdk.util.Assert;

import java.util.UUID;

/**
 * Used to initiate a task with streaming.
 */
public final class SendStreamingMessageRequest extends StreamingJSONRPCRequest<MessageSendParams> {

    public static final String METHOD = "message/stream";

    public SendStreamingMessageRequest(String jsonrpc, Object id, String method, MessageSendParams params) {
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

    public SendStreamingMessageRequest(Object id,  MessageSendParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
            private String jsonrpc;
            private Object id;
            private String method = METHOD;
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

            public SendStreamingMessageRequest build() {
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
                return new SendStreamingMessageRequest(jsonrpc, id, method, params);
            }
        }
    }
