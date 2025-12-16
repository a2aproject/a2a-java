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

    /** The JSON-RPC method name. */
    public static final String METHOD = "SetTaskPushNotificationConfig";

    /**
     * Constructs request with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param params the request parameters
     */
    public SetTaskPushNotificationConfigRequest(String jsonrpc, Object id, TaskPushNotificationConfig params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**
     * Constructs request with ID and parameters.
     *
     * @param id the request ID
     * @param taskPushConfig the task push notification configuration
     */
    public SetTaskPushNotificationConfigRequest(String id, TaskPushNotificationConfig taskPushConfig) {
        this(null, id, taskPushConfig);
    }

    /**
     * Creates a new builder for {@link SetTaskPushNotificationConfigRequest}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing instances.
     */
    public static class Builder {
        private String jsonrpc;
        private Object id;
        private TaskPushNotificationConfig params;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the JSON-RPC version.
         *
         * @param jsonrpc the JSON-RPC version
         * @return this builder for method chaining
         */
        public SetTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request ID.
         *
         * @param id the request ID
         * @return this builder for method chaining
         */
        public SetTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the request parameters.
         *
         * @param params the request parameters
         * @return this builder for method chaining
         */
        public SetTaskPushNotificationConfigRequest.Builder params(TaskPushNotificationConfig params) {
            this.params = params;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return a new instance
         */
        public SetTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SetTaskPushNotificationConfigRequest(jsonrpc, id, params);
        }
    }
}
