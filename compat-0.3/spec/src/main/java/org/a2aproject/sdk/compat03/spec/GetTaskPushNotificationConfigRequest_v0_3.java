package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.compat03.util.Utils_v0_3;

import java.util.UUID;

/**
 * A get task push notification request.
 */
public final class GetTaskPushNotificationConfigRequest_v0_3 extends NonStreamingJSONRPCRequest_v0_3<GetTaskPushNotificationConfigParams_v0_3> {

    public static final String METHOD = "tasks/pushNotificationConfig/get";

    public GetTaskPushNotificationConfigRequest_v0_3(String jsonrpc, Object id, String method, GetTaskPushNotificationConfigParams_v0_3 params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid GetTaskPushNotificationRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils_v0_3.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public GetTaskPushNotificationConfigRequest_v0_3(String id, GetTaskPushNotificationConfigParams_v0_3 params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private GetTaskPushNotificationConfigParams_v0_3 params;

        public GetTaskPushNotificationConfigRequest_v0_3.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetTaskPushNotificationConfigRequest_v0_3.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public GetTaskPushNotificationConfigRequest_v0_3.Builder method(String method) {
            this.method = method;
            return this;
        }

        public GetTaskPushNotificationConfigRequest_v0_3.Builder params(GetTaskPushNotificationConfigParams_v0_3 params) {
            this.params = params;
            return this;
        }

        public GetTaskPushNotificationConfigRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskPushNotificationConfigRequest_v0_3(jsonrpc, id, method, params);
        }
    }
}
