package org.a2aproject.sdk.jsonrpc.common.wrappers;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskState;

/**
 * JSON-RPC response for task cancellation requests.
 * <p>
 * This response contains the updated {@link Task} object after cancellation, typically
 * showing {@link TaskState#TASK_STATE_CANCELED} status if the cancellation was successful.
 * <p>
 * If the task cannot be canceled (e.g., already completed) or is not found, the error
 * field will contain a {@link org.a2aproject.sdk.spec.A2AError} such as {@link TaskNotCancelableError} or
 * {@link TaskNotFoundError}.
 *
 * @see CancelTaskRequest for the corresponding request
 * @see Task for the task structure
 * @see TaskNotCancelableError for the error when cancellation fails
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */

public final class CancelTaskResponse extends A2AResponse<Task> {

    /**
     * Constructs a CancelTaskResponse with full parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the task result
     * @param error the error if any
     */
    public CancelTaskResponse(String jsonrpc, Object id, Task result, A2AError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    /**
     * Constructs a CancelTaskResponse with an error.
     *
     * @param id the request ID
     * @param error the error
     */
    public CancelTaskResponse(Object id, A2AError error) {
        this(null, id, null, error);
    }


    /**
     * Constructs a CancelTaskResponse with a successful result.
     *
     * @param id the request ID
     * @param result the task result
     */
    public CancelTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
