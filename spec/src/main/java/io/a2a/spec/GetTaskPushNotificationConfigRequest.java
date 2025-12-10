package io.a2a.spec;

import java.util.UUID;

/**
 * JSON-RPC request to retrieve push notification configuration for a task.
 * <p>
 * This request retrieves the currently configured push notification endpoint and settings
 * for a specific task, allowing clients to verify or inspect the notification configuration.
 * <p>
 * This class implements the JSON-RPC {@code tasks/pushNotificationConfig/get} method.
 *
 * @see GetTaskPushNotificationConfigResponse for the response
 * @see GetTaskPushNotificationConfigParams for the parameter structure
 * @see TaskPushNotificationConfig for the returned configuration
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<GetTaskPushNotificationConfigParams> {

    public static final String METHOD = "GetTaskPushNotificationConfig";

    public GetTaskPushNotificationConfigRequest(String jsonrpc, Object id, GetTaskPushNotificationConfigParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    public GetTaskPushNotificationConfigRequest(String id, GetTaskPushNotificationConfigParams params) {
        this(null, id, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private GetTaskPushNotificationConfigParams params;

        public GetTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * @deprecated
         */
        public GetTaskPushNotificationConfigRequest.Builder method(String method) {
            return this;
        }

        public GetTaskPushNotificationConfigRequest.Builder params(GetTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        public GetTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskPushNotificationConfigRequest(jsonrpc, id, params);
        }
    }
}
