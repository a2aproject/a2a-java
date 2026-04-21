package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

import java.util.UUID;


import org.a2aproject.sdk.util.Assert;

/**
 * A request that can be used to cancel a task.
 */
public final class CancelTaskRequest_v0_3 extends NonStreamingJSONRPCRequest_v0_3<TaskIdParams_v0_3> {

    public static final String METHOD = "tasks/cancel";

    public CancelTaskRequest_v0_3(String jsonrpc, Object id, String method, TaskIdParams_v0_3 params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid CancelTaskRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public CancelTaskRequest_v0_3(Object id, TaskIdParams_v0_3 params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskIdParams_v0_3 params;

        public CancelTaskRequest_v0_3.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public CancelTaskRequest_v0_3.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public CancelTaskRequest_v0_3.Builder method(String method) {
            this.method = method;
            return this;
        }

        public CancelTaskRequest_v0_3.Builder params(TaskIdParams_v0_3 params) {
            this.params = params;
            return this;
        }

        public CancelTaskRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new CancelTaskRequest_v0_3(jsonrpc, id, method, params);
        }
    }
}
