package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

/**
 * An A2A-specific error indicating that the requested task ID was not found.
 */
public class TaskNotFoundError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = A2AErrorCodes.TASK_NOT_FOUND_ERROR_CODE;

    public TaskNotFoundError() {
        this(null, null, null);
    }

    public TaskNotFoundError(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, A2AErrorCodes.TASK_NOT_FOUND_ERROR_CODE),
                defaultIfNull(message, "Task not found"),
                data);
    }
}
