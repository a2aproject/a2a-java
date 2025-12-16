package io.a2a.spec;

import java.util.UUID;

/**
 * JSON-RPC request to retrieve task information by ID.
 * <p>
 * This request queries the agent for the current state of a specific task, including its
 * status, artifacts, messages, and other task metadata. Clients use this to check task
 * progress or retrieve completed task results.
 * <p>
 * This class implements the JSON-RPC {@code tasks/get} method as specified in the A2A Protocol.
 *
 * @see GetTaskResponse for the corresponding response
 * @see TaskQueryParams for the parameter structure
 * @see Task for the returned task structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetTaskRequest extends NonStreamingJSONRPCRequest<TaskQueryParams> {

    /** The JSON-RPC method name. */


    public static final String METHOD = "GetTask";

    /**


     * Constructs request with all parameters.


     *


     * @param jsonrpc the JSON-RPC version


     * @param id the request ID


     * @param params the request parameters


     */


    public GetTaskRequest(String jsonrpc, Object id, TaskQueryParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**


     * Constructs request with ID and parameters.


     *


     * @param id the request ID


     * @param params the request parameters


     */


    public GetTaskRequest(Object id, TaskQueryParams params) {
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
        private TaskQueryParams params;

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


        /**
         * Sets the jsonrpc.
         *
         * @param jsonrpc the jsonrpc
         * @return this builder for method chaining
         */
        public GetTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**


         * Sets the request ID.


         *


         * @param id the request ID


         * @return this builder for method chaining


         */


        /**
         * Sets the id.
         *
         * @param id the id
         * @return this builder for method chaining
         */
        public GetTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**


         * Sets the request parameters.


         *


         * @param params the request parameters


         * @return this builder for method chaining


         */


        /**
         * Sets the params.
         *
         * @param params the params
         * @return this builder for method chaining
         */
        public GetTaskRequest.Builder params(TaskQueryParams params) {
            this.params = params;
            return this;
        }

        /**


         * Builds the instance.


         *


         * @return a new instance


         */


        public GetTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskRequest(jsonrpc, id, params);
        }
    }
}
