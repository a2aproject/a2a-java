package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

/**
 * An A2A-specific error indicating that the task is in a state where it cannot be canceled.
 */
public class TaskNotCancelableError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32002;

    public TaskNotCancelableError() {
        this(null, null, null);
    }

    public TaskNotCancelableError(
            Integer code,
            String message,
            Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Task cannot be canceled"),
                data);
    }

    public TaskNotCancelableError(String message) {
        this(null, message, null);
    }

}
