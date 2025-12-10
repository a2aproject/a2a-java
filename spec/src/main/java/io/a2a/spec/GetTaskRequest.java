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

    public static final String METHOD = "GetTask";

    public GetTaskRequest(String jsonrpc, Object id, TaskQueryParams params) {
        super(jsonrpc, METHOD, id, params);
    }

    public GetTaskRequest(Object id, TaskQueryParams params) {
        this(null, id, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private TaskQueryParams params;

        public GetTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * @deprecated
         */
        public GetTaskRequest.Builder method(String method) {
            return this;
        }

        public GetTaskRequest.Builder params(TaskQueryParams params) {
            this.params = params;
            return this;
        }

        public GetTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskRequest(jsonrpc, id, params);
        }
    }
}
