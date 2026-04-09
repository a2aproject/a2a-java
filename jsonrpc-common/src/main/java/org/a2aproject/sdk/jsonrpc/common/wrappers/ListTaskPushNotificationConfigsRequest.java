package org.a2aproject.sdk.jsonrpc.common.wrappers;


import java.util.UUID;

import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;

import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;

/**
 * JSON-RPC request to list all push notification configurations for a task.
 * <p>
 * This request retrieves all configured push notification endpoints for a specific task,
 * allowing clients to enumerate all active notification subscriptions.
 * <p>
 * This class implements the JSON-RPC {@code tasks/pushNotificationConfig/list} method.
 *
 * @see ListTaskPushNotificationConfigsResponse for the response
 * @see ListTaskPushNotificationConfigsParams for the parameter structure
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class ListTaskPushNotificationConfigsRequest extends NonStreamingJSONRPCRequest<ListTaskPushNotificationConfigsParams> {

    /**
     * Constructs request with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param params the request parameters
     */
    public ListTaskPushNotificationConfigsRequest(String jsonrpc, Object id, ListTaskPushNotificationConfigsParams params) {
        super(jsonrpc, LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, id, params);
    }

    /**
     * Constructs request with ID and parameters.
     *
     * @param id the request ID
     * @param params the request parameters
     */
    public ListTaskPushNotificationConfigsRequest(String id, ListTaskPushNotificationConfigsParams params) {
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
        private ListTaskPushNotificationConfigsParams params;

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
        public Builder params(ListTaskPushNotificationConfigsParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return a new instance
         */
        public ListTaskPushNotificationConfigsRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTaskPushNotificationConfigsRequest(jsonrpc, id, params);
        }
    }
}
