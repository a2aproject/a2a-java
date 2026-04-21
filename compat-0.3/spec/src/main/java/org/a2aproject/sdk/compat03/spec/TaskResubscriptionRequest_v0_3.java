package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

import org.a2aproject.sdk.util.Assert;

import java.util.UUID;

/**
 * Used to resubscribe to a task.
 */
public final class TaskResubscriptionRequest_v0_3 extends StreamingJSONRPCRequest_v0_3<TaskIdParams_v0_3> {

    public static final String METHOD = "tasks/resubscribe";

    public TaskResubscriptionRequest_v0_3(String jsonrpc, Object id, String method, TaskIdParams_v0_3 params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid TaskResubscriptionRequest method");
        }
        Assert.checkNotNullParam("params", params);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.method = method;
        this.params = params;
    }

    public TaskResubscriptionRequest_v0_3(Object id, TaskIdParams_v0_3 params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskIdParams_v0_3 params;

        public TaskResubscriptionRequest_v0_3.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public TaskResubscriptionRequest_v0_3.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public TaskResubscriptionRequest_v0_3.Builder method(String method) {
            this.method = method;
            return this;
        }

        public TaskResubscriptionRequest_v0_3.Builder params(TaskIdParams_v0_3 params) {
            this.params = params;
            return this;
        }

        public TaskResubscriptionRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new TaskResubscriptionRequest_v0_3(jsonrpc, id, method, params);
        }
    }
}
