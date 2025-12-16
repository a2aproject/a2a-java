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

    /** The JSON-RPC method name. */
    public static final String METHOD = "GetTaskPushNotificationConfig";

    /**
     * Constructs request with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param params the request parameters
     */
    public GetTaskPushNotificationConfigRequest(String jsonrpc, Object id, GetTaskPushNotificationConfigParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**
     * Constructs request with ID and parameters.
     *
     * @param id the request ID
     * @param params the request parameters
     */
    public GetTaskPushNotificationConfigRequest(String id, GetTaskPushNotificationConfigParams params) {
        this(null, id, params);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
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
        private GetTaskPushNotificationConfigParams params;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the jsonrpc.
         *
         * @param jsonrpc the jsonrpc
         * @return this builder for method chaining
         */
        public GetTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request ID.
         *
         * @param id the request ID
         * @return this builder for method chaining
         */
        public GetTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the request parameters.
         *
         * @param params the request parameters
         * @return this builder for method chaining
         */
        public GetTaskPushNotificationConfigRequest.Builder params(GetTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return a new instance
         */
        public GetTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskPushNotificationConfigRequest(jsonrpc, id, params);
        }
    }
}
