package org.a2aproject.sdk.compat03.spec;

import java.util.UUID;

import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.compat03.util.Utils_v0_3;

/**
 * A delete task push notification config request.
 */
public final class DeleteTaskPushNotificationConfigRequest_v0_3 extends NonStreamingJSONRPCRequest_v0_3<DeleteTaskPushNotificationConfigParams_v0_3> {

    public static final String METHOD = "tasks/pushNotificationConfig/delete";

    public DeleteTaskPushNotificationConfigRequest_v0_3(String jsonrpc, Object id, String method, DeleteTaskPushNotificationConfigParams_v0_3 params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid DeleteTaskPushNotificationConfigRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils_v0_3.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public DeleteTaskPushNotificationConfigRequest_v0_3(String id, DeleteTaskPushNotificationConfigParams_v0_3 params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private DeleteTaskPushNotificationConfigParams_v0_3 params;

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

        public Builder params(DeleteTaskPushNotificationConfigParams_v0_3 params) {
            this.params = params;
            return this;
        }

        public DeleteTaskPushNotificationConfigRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new DeleteTaskPushNotificationConfigRequest_v0_3(jsonrpc, id, method, params);
        }
    }
}
