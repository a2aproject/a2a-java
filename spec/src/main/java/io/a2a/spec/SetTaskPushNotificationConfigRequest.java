package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import io.a2a.util.Assert;

/**
 * Used to set a task push notification request.
 */
public final class SetTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<TaskPushNotificationConfig> {

    public static final String METHOD = "tasks/pushNotificationConfig/set";

    public SetTaskPushNotificationConfigRequest(String jsonrpc, Object id, String method, TaskPushNotificationConfig params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid SetTaskPushNotificationRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public SetTaskPushNotificationConfigRequest(String id, TaskPushNotificationConfig taskPushConfig) {
        this(null, id, METHOD, taskPushConfig);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskPushNotificationConfig params;

        public SetTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public SetTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public SetTaskPushNotificationConfigRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public SetTaskPushNotificationConfigRequest.Builder params(TaskPushNotificationConfig params) {
            this.params = params;
            return this;
        }

        public SetTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SetTaskPushNotificationConfigRequest(jsonrpc, id, method, params);
        }
    }
}
