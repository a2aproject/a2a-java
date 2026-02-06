package io.a2a.jsonrpc.common.wrappers;

import static io.a2a.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;

import java.util.UUID;

import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskIdParams;

/**
 * JSON-RPC request to subscribe to an ongoing or completed task's event stream.
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

    /**
     * Constructs request with all parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param params the request parameters
     */
    public SubscribeToTaskRequest(String jsonrpc, Object id, TaskIdParams params) {
        super(jsonrpc, SUBSCRIBE_TO_TASK_METHOD, id == null ? UUID.randomUUID().toString() : id, params);
    }

    /**
     * Constructs request with ID and parameters.
     *
     * @param id the request ID
     * @param params the request parameters
     */
    public SubscribeToTaskRequest(Object id, TaskIdParams params) {
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
        private TaskIdParams params;

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
        public SubscribeToTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request ID.
         *
         * @param id the request ID
         * @return this builder for method chaining
         */
        public SubscribeToTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the request parameters.
         *
         * @param params the request parameters
         * @return this builder for method chaining
         */
        public SubscribeToTaskRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return a new instance
         */
        public SubscribeToTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SubscribeToTaskRequest(jsonrpc, id, params);
        }
    }
}
