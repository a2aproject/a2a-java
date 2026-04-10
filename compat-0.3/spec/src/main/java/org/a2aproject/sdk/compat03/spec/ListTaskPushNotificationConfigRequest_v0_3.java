package org.a2aproject.sdk.compat03.spec;

import java.util.UUID;

import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.compat03.util.Utils_v0_3;

/**
 * A list task push notification config request.
 */
public final class ListTaskPushNotificationConfigRequest_v0_3 extends NonStreamingJSONRPCRequest_v0_3<ListTaskPushNotificationConfigParams_v0_3> {

    public static final String METHOD = "tasks/pushNotificationConfig/list";

    public ListTaskPushNotificationConfigRequest_v0_3(String jsonrpc, Object id, String method, ListTaskPushNotificationConfigParams_v0_3 params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid ListTaskPushNotificationConfigRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils_v0_3.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public ListTaskPushNotificationConfigRequest_v0_3(String id, ListTaskPushNotificationConfigParams_v0_3 params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private ListTaskPushNotificationConfigParams_v0_3 params;

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

        public Builder params(ListTaskPushNotificationConfigParams_v0_3 params) {
            this.params = params;
            return this;
        }

        public ListTaskPushNotificationConfigRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTaskPushNotificationConfigRequest_v0_3(jsonrpc, id, method, params);
        }
    }
}
