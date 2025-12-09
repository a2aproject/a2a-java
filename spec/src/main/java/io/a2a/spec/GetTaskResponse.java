package io.a2a.spec;

/**
 * JSON-RPC response containing requested task information.
 * <p>
 * This response returns the full {@link Task} object for a task ID queried via
 * {@link GetTaskRequest}, including all task metadata, status, artifacts, and messages.
 * <p>
 * If the task is not found or an error occurs, the error field will be populated with
 * a {@link JSONRPCError} (typically {@link TaskNotFoundError}) instead of a result.
 *
 * @see GetTaskRequest for the corresponding request
 * @see Task for the task structure
 * @see TaskNotFoundError for the common error case
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetTaskResponse extends JSONRPCResponse<Task> {

    public GetTaskResponse(String jsonrpc, Object id, Task result, JSONRPCError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    public GetTaskResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public GetTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
