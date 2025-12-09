package io.a2a.spec;

import java.util.UUID;

import io.a2a.util.Assert;
import io.a2a.util.Utils;

/**
 * JSON-RPC request to list all push notification configurations for a task.
 * <p>
 * This request retrieves all configured push notification endpoints for a specific task,
 * allowing clients to enumerate all active notification subscriptions.
 * <p>
 * This class implements the JSON-RPC {@code tasks/pushNotificationConfig/list} method.
 *
 * @see ListTaskPushNotificationConfigResponse for the response
 * @see ListTaskPushNotificationConfigParams for the parameter structure
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class ListTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<ListTaskPushNotificationConfigParams> {

    public static final String METHOD = "ListTaskPushNotificationConfig";

    public ListTaskPushNotificationConfigRequest(String jsonrpc, Object id, String method, ListTaskPushNotificationConfigParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid ListTaskPushNotificationConfigRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public ListTaskPushNotificationConfigRequest(String id, ListTaskPushNotificationConfigParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private ListTaskPushNotificationConfigParams params;

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

        public Builder params(ListTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        public ListTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTaskPushNotificationConfigRequest(jsonrpc, id, method, params);
        }
    }
}
