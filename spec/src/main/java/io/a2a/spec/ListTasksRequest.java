package io.a2a.spec;

import java.util.UUID;

/**
 * A list tasks request.
 */
public final class ListTasksRequest extends NonStreamingJSONRPCRequest<ListTasksParams> {

    /** The JSON-RPC method name. */


    public static final String METHOD = "ListTask";

    /**


     * Constructs request with all parameters.


     *


     * @param jsonrpc the JSON-RPC version


     * @param id the request ID


     * @param params the request parameters


     */


    public ListTasksRequest(String jsonrpc, Object id, ListTasksParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    /**


     * Constructs request with ID and parameters.


     *


     * @param id the request ID


     * @param params the request parameters


     */


    public ListTasksRequest(Object id, ListTasksParams params) {
        this(JSONRPC_VERSION, id, params);
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
        private ListTasksParams params;

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
        public Builder params(ListTasksParams params) {
            this.params = params;
            return this;
        }

        /**


         * Builds the instance.


         *


         * @return a new instance


         */


        public ListTasksRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTasksRequest(jsonrpc, id, params);
        }
    }
}
