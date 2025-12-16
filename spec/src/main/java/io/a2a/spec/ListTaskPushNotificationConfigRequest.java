package io.a2a.spec;

import java.util.UUID;

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

    /** The JSON-RPC method name. */


    public static final String METHOD = "ListTaskPushNotificationConfig";

    /**


     * Constructs request with all parameters.


     *


     * @param jsonrpc the JSON-RPC version


     * @param id the request ID


     * @param params the request parameters


     */


    public ListTaskPushNotificationConfigRequest(String jsonrpc, Object id, ListTaskPushNotificationConfigParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**
     * Constructs request with ID and parameters.
     *
     * @param id the request ID
     * @param params the request parameters
     */
    public ListTaskPushNotificationConfigRequest(String id, ListTaskPushNotificationConfigParams params) {
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
        private ListTaskPushNotificationConfigParams params;

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
        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the id.
         *
         * @param id the id
         * @return this builder for method chaining
         */
        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the params.
         *
         * @param params the params
         * @return this builder for method chaining
         */
        public Builder params(ListTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        /**


         * Builds the instance.


         *


         * @return a new instance


         */


        public ListTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTaskPushNotificationConfigRequest(jsonrpc, id, params);
        }
    }
}
