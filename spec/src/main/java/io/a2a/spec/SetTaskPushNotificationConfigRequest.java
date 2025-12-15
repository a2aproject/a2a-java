package io.a2a.spec;

import java.util.UUID;

/**
 * JSON-RPC request to configure push notifications for a specific task.
 * <p>
 * This request registers or updates the push notification endpoint for a task, enabling
 * the agent to send asynchronous updates (status changes, artifact additions) to the
 * specified URL without requiring client polling.
 * <p>
 * This class implements the JSON-RPC {@code tasks/pushNotificationConfig/set} method.
 *
 * @see SetTaskPushNotificationConfigResponse for the response
 * @see TaskPushNotificationConfig for the parameter structure
 * @see PushNotificationConfig for notification endpoint details
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class SetTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<TaskPushNotificationConfig> {

    public static final String METHOD = "SetTaskPushNotificationConfig";

    public SetTaskPushNotificationConfigRequest(String jsonrpc, Object id, TaskPushNotificationConfig params) {
        super(jsonrpc, METHOD, id, params);
    }

    public SetTaskPushNotificationConfigRequest(String id, TaskPushNotificationConfig taskPushConfig) {
        this(null, id, taskPushConfig);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private TaskPushNotificationConfig params;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        public SetTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public SetTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * @deprecated
         */
        public SetTaskPushNotificationConfigRequest.Builder method(String method) {
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
            return new SetTaskPushNotificationConfigRequest(jsonrpc, id, params);
        }
    }
}
