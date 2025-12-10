package io.a2a.spec;

import java.util.UUID;

/**
 * JSON-RPC request to resubscribe to an ongoing or completed task's event stream.
 * <p>
 * This request allows clients to reconnect to a task and receive its events, enabling
 * recovery from disconnections or retrieval of missed updates. The agent will stream
 * events for the task starting from its current state.
 * <p>
 * Resubscription is particularly useful for:
 * <ul>
 *   <li>Recovering from network interruptions without losing task context</li>
 *   <li>Multiple clients observing the same task</li>
 *   <li>Retrieving final results for completed tasks</li>
 * </ul>
 * <p>
 * This class implements the JSON-RPC {@code SubscribeToTask} method as specified in the A2A Protocol.
 *
 * @see TaskIdParams for the parameter structure
 * @see StreamingEventKind for the types of events that can be streamed
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class SubscribeToTaskRequest extends StreamingJSONRPCRequest<TaskIdParams> {

    public static final String METHOD = "SubscribeToTask";

    public SubscribeToTaskRequest(String jsonrpc, Object id, TaskIdParams params) {
        super(jsonrpc, METHOD, id == null ? UUID.randomUUID().toString() : id, params);
    }

    public SubscribeToTaskRequest(Object id, TaskIdParams params) {
        this(null, id, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private TaskIdParams params;

        public SubscribeToTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public SubscribeToTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * @deprecated
         */
        public SubscribeToTaskRequest.Builder method(String method) {
            return this;
        }

        public SubscribeToTaskRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        public SubscribeToTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SubscribeToTaskRequest(jsonrpc, id, params);
        }
    }
}
