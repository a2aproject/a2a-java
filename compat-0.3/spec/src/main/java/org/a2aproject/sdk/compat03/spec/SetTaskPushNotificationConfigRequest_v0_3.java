package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

import java.util.UUID;

import org.a2aproject.sdk.util.Assert;

/**
 * Used to set a task push notification request.
 */
public final class SetTaskPushNotificationConfigRequest_v0_3 extends NonStreamingJSONRPCRequest_v0_3<TaskPushNotificationConfig_v0_3> {

    public static final String METHOD = "tasks/pushNotificationConfig/set";

    public SetTaskPushNotificationConfigRequest_v0_3(String jsonrpc, Object id, String method, TaskPushNotificationConfig_v0_3 params) {
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

    public SetTaskPushNotificationConfigRequest_v0_3(String id, TaskPushNotificationConfig_v0_3 taskPushConfig) {
        this(null, id, METHOD, taskPushConfig);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskPushNotificationConfig_v0_3 params;

        public SetTaskPushNotificationConfigRequest_v0_3.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public SetTaskPushNotificationConfigRequest_v0_3.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public SetTaskPushNotificationConfigRequest_v0_3.Builder method(String method) {
            this.method = method;
            return this;
        }

        public SetTaskPushNotificationConfigRequest_v0_3.Builder params(TaskPushNotificationConfig_v0_3 params) {
            this.params = params;
            return this;
        }

        public SetTaskPushNotificationConfigRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SetTaskPushNotificationConfigRequest_v0_3(jsonrpc, id, method, params);
        }
    }
}
