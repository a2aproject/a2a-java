package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;


import io.a2a.util.Assert;

/**
 * A request that can be used to cancel a task.
 */
public final class CancelTaskRequest extends NonStreamingJSONRPCRequest<TaskIdParams> {

    public static final String METHOD = "tasks/cancel";

    public CancelTaskRequest(String jsonrpc, Object id, String method, TaskIdParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid CancelTaskRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isValidJsonRpcId(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public CancelTaskRequest(Object id, TaskIdParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskIdParams params;

        public CancelTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public CancelTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public CancelTaskRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public CancelTaskRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        public CancelTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new CancelTaskRequest(jsonrpc, id, method, params);
        }
    }
}
