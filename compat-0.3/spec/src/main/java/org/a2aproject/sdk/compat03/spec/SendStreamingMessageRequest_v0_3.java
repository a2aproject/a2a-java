package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

import org.a2aproject.sdk.util.Assert;

import java.util.UUID;

/**
 * Used to initiate a task with streaming.
 */
public final class SendStreamingMessageRequest_v0_3 extends StreamingJSONRPCRequest_v0_3<MessageSendParams_v0_3> {

    public static final String METHOD = "message/stream";

    public SendStreamingMessageRequest_v0_3(String jsonrpc, Object id, String method, MessageSendParams_v0_3 params) {
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

    public void check() {
        if (jsonrpc != null && !jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (!method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid SendStreamingMessageRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        params.check();
    }

    public SendStreamingMessageRequest_v0_3(Object id, MessageSendParams_v0_3 params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
            private String jsonrpc;
            private Object id;
            private String method = METHOD;
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

            public SendStreamingMessageRequest_v0_3 build() {
                if (id == null) {
                    id = UUID.randomUUID().toString();
                }
                return new SendStreamingMessageRequest_v0_3(jsonrpc, id, method, params);
            }
        }
    }
