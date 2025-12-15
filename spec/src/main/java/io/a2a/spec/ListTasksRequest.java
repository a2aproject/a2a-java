package io.a2a.spec;

import java.util.UUID;

/**
 * A list tasks request.
 */
public final class ListTasksRequest extends NonStreamingJSONRPCRequest<ListTasksParams> {

    public static final String METHOD = "ListTask";

    public ListTasksRequest(String jsonrpc, Object id, ListTasksParams params) {
        super(jsonrpc, METHOD, id, params);
    }

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

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private ListTasksParams params;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * @deprecated
         */
        public Builder method(String method) {
            return this;
        }

        public Builder params(ListTasksParams params) {
            this.params = params;
            return this;
        }

        public ListTasksRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTasksRequest(jsonrpc, id, params);
        }
    }
}
