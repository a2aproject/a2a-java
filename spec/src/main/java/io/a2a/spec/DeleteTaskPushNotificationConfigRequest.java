package io.a2a.spec;

import java.util.UUID;

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

    /** The JSON-RPC method name for deleting push notification configurations. */
    public static final String METHOD = "DeleteTaskPushNotificationConfig";

    /**
     * Creates a new DeleteTaskPushNotificationConfigRequest with the specified JSON-RPC parameters.
     *
     * @param jsonrpc the JSON-RPC version (defaults to "2.0" if null)
     * @param id the request identifier (string, integer, or null)
     * @param params the request parameters containing task and config IDs
     * @throws IllegalArgumentException if jsonrpc version is invalid, method is not "DeleteTaskPushNotificationConfig", or id is not a string/integer/null
     */
    public DeleteTaskPushNotificationConfigRequest(String jsonrpc, Object id, DeleteTaskPushNotificationConfigParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**
     * Creates a new DeleteTaskPushNotificationConfigRequest with default JSON-RPC version and method.
     *
     * @param id the request identifier (string, integer, or null)
     * @param params the request parameters containing task and config IDs
     * @throws IllegalArgumentException if id is not a string/integer/null
     */
    public DeleteTaskPushNotificationConfigRequest(String id, DeleteTaskPushNotificationConfigParams params) {
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
     * Builder for constructing {@link DeleteTaskPushNotificationConfigRequest} instances.
     * <p>
     * Provides a fluent API for setting request parameters. If no id is provided,
     * a random UUID will be generated when {@link #build()} is called.
     */
    public static class Builder {
        private String jsonrpc;
        private Object id;
        private DeleteTaskPushNotificationConfigParams params;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the JSON-RPC protocol version.
         *
         * @param jsonrpc the JSON-RPC version (optional, defaults to "2.0")
         * @return this builder for method chaining
         */
        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request identifier.
         *
         * @param id the request identifier (string, integer, or null; if null, a UUID will be generated)
         * @return this builder for method chaining
         */
        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the request parameters.
         *
         * @param params the request parameters containing task and config IDs (required)
         * @return this builder for method chaining
         */
        public Builder params(DeleteTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds a new {@link DeleteTaskPushNotificationConfigRequest} from the current builder state.
         * <p>
         * If no id was provided, a random UUID will be generated.
         *
         * @return a new DeleteTaskPushNotificationConfigRequest instance
         * @throws IllegalArgumentException if validation fails (invalid method, invalid id type)
         */
        public DeleteTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new DeleteTaskPushNotificationConfigRequest(jsonrpc, id, params);
        }
    }
}
