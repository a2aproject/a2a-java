package io.a2a.spec;

import java.util.UUID;

import io.a2a.util.Assert;
import io.a2a.util.Utils;

/**
 * A delete task push notification config request.
 */
public final class DeleteTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<DeleteTaskPushNotificationConfigParams> {

    public static final String METHOD = "tasks/pushNotificationConfig/delete";

    public DeleteTaskPushNotificationConfigRequest(String jsonrpc, Object id, String method, DeleteTaskPushNotificationConfigParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid DeleteTaskPushNotificationConfigRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public DeleteTaskPushNotificationConfigRequest(String id, DeleteTaskPushNotificationConfigParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private DeleteTaskPushNotificationConfigParams params;

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

        public Builder params(DeleteTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        public DeleteTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new DeleteTaskPushNotificationConfigRequest(jsonrpc, id, method, params);
        }
    }
}
