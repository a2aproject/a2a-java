package io.a2a.spec;

/**
 * JSON-RPC response for task cancellation requests.
 * <p>
 * This response contains the updated {@link Task} object after cancellation, typically
 * showing {@link TaskState#CANCELED} status if the cancellation was successful.
 * <p>
 * If the task cannot be canceled (e.g., already completed) or is not found, the error
 * field will contain a {@link JSONRPCError} such as {@link TaskNotCancelableError} or
 * {@link TaskNotFoundError}.
 *
 * @see CancelTaskRequest for the corresponding request
 * @see Task for the task structure
 * @see TaskNotCancelableError for the error when cancellation fails
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */

public final class CancelTaskResponse extends JSONRPCResponse<Task> {

    public CancelTaskResponse(String jsonrpc, Object id, Task result, JSONRPCError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    public CancelTaskResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }


    public CancelTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
