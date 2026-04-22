package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import io.a2a.util.Assert;

import java.util.UUID;

/**
 * Used to resubscribe to a task.
 */
public final class TaskResubscriptionRequest extends StreamingJSONRPCRequest<TaskIdParams> {

    public static final String METHOD = "tasks/resubscribe";

    public TaskResubscriptionRequest(String jsonrpc, Object id, String method, TaskIdParams params) {
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

    public TaskResubscriptionRequest(Object id, TaskIdParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskIdParams params;

        public TaskResubscriptionRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public TaskResubscriptionRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public TaskResubscriptionRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public TaskResubscriptionRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        public TaskResubscriptionRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new TaskResubscriptionRequest(jsonrpc, id, method, params);
        }
    }
}
