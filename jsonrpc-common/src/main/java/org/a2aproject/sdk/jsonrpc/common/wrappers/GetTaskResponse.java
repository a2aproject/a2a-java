package org.a2aproject.sdk.jsonrpc.common.wrappers;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotFoundError;

/**
 * JSON-RPC response containing requested task information.
 * <p>
 * This response returns the full {@link Task} object for a task ID queried via
 * {@link GetTaskRequest}, including all task metadata, status, artifacts, and messages.
 * <p>
 * If the task is not found or an error occurs, the error field will be populated with
 * a {@link A2AError} (typically {@link TaskNotFoundError}) instead of a result.
 *
 * @see GetTaskRequest for the corresponding request
 * @see Task for the task structure
 * @see TaskNotFoundError for the common error case
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetTaskResponse extends A2AResponse<Task> {

    /**
     * Constructs response with full parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the task result
     * @param error the error if any
     */
    public GetTaskResponse(String jsonrpc, Object id, Task result, A2AError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    /**
     * Constructs error response.
     *
     * @param id the request ID
     * @param error the error
     */
    public GetTaskResponse(Object id, A2AError error) {
        this(null, id, null, error);
    }

    /**
     * Constructs successful response.
     *
     * @param id the request ID
     * @param result the task result
     */
    public GetTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
