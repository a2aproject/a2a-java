package io.a2a.spec;

import java.util.UUID;

import io.a2a.util.Assert;
import io.a2a.util.Utils;

/**
 * JSON-RPC request to delete a push notification configuration from a task.
 * <p>
 * This request removes a specific push notification endpoint configuration from a task,
 * stopping future notifications to that endpoint. The task will continue execution, but
 * no longer send updates to the deleted notification URL.
 * <p>
 * This class implements the JSON-RPC {@code tasks/pushNotificationConfig/delete} method.
 *
 * @see DeleteTaskPushNotificationConfigResponse for the response
 * @see DeleteTaskPushNotificationConfigParams for the parameter structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class DeleteTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<DeleteTaskPushNotificationConfigParams> {

    public static final String METHOD = "DeleteTaskPushNotificationConfig";

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
