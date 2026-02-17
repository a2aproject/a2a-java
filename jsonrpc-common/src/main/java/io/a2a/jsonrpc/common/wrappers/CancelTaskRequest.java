package io.a2a.jsonrpc.common.wrappers;

import static io.a2a.spec.A2AMethods.CANCEL_TASK_METHOD;

import java.util.UUID;

import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;

/**
 * JSON-RPC request to cancel an in-progress task.
 * <p>
 * This request instructs the agent to cancel execution of a specific task identified by ID.
 * The agent should stop processing, clean up resources, and transition the task to
 * {@link TaskState#TASK_STATE_CANCELED} state if cancellation is possible.
 * <p>
 * Not all tasks can be canceled (e.g., already completed tasks), which may result in
 * a {@link TaskNotCancelableError}.
 * <p>
 * This class implements the JSON-RPC {@code tasks/cancel} method as specified in the A2A Protocol.
 *
 * @see CancelTaskResponse for the corresponding response
 * @see TaskIdParams for the parameter structure
 * @see TaskNotCancelableError for the error when cancellation is not possible
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class CancelTaskRequest extends NonStreamingJSONRPCRequest<TaskIdParams> {

    /**
     * Creates a new CancelTaskRequest with the specified JSON-RPC parameters.
     *
     * @param jsonrpc the JSON-RPC version (defaults to "2.0" if null)
     * @param id the request identifier (string, integer, or null)
     * @param params the request parameters containing the task ID
     * @throws IllegalArgumentException if jsonrpc version is invalid, method is not "CancelTask", params is null, or id is not a String/Integer/null
     */
    public CancelTaskRequest(String jsonrpc, Object id, TaskIdParams params) {
        super(jsonrpc, CANCEL_TASK_METHOD, id, params);
    }

    /**
     * Creates a new CancelTaskRequest with default JSON-RPC version and method.
     *
     * @param id the request identifier (string, integer, or null)
     * @param params the request parameters containing the task ID
     * @throws IllegalArgumentException if params is null or id is not a string/integer/null
     */
    public CancelTaskRequest(Object id, TaskIdParams params) {
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
     * Builder for constructing {@link CancelTaskRequest} instances.
     * <p>
     * Provides a fluent API for setting request parameters. If no id is provided,
     * a random UUID will be generated when {@link #build()} is called.
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
         * Sets the JSON-RPC protocol version.
         *
         * @param jsonrpc the JSON-RPC version (optional, defaults to "2.0")
         * @return this builder for method chaining
         */

        public CancelTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request identifier.
         *
         * @param id the request identifier (string, integer, or null; if null, a UUID will be generated)
         * @return this builder for method chaining
         */
        public CancelTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the request parameters containing the task ID to cancel.
         *
         * @param params the request parameters (required)
         * @return this builder for method chaining
         */

        public CancelTaskRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds a new {@link CancelTaskRequest} from the current builder state.
         * <p>
         * If no id was provided, a random UUID will be generated.
         *
         * @return a new CancelTaskRequest instance
         * @throws IllegalArgumentException if validation fails (invalid method, null params, invalid id type)
         */
        public CancelTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new CancelTaskRequest(jsonrpc, id, params);
        }
    }
}
