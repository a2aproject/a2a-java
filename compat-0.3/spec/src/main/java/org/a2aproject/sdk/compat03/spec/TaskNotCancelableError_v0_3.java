package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

/**
 * An A2A-specific error indicating that the task is in a state where it cannot be canceled.
 */
public class TaskNotCancelableError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = -32002;

    public TaskNotCancelableError_v0_3() {
        this(null, null, null);
    }

    public TaskNotCancelableError_v0_3(
            Integer code,
            String message,
            Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Task cannot be canceled"),
                data);
    }

    public TaskNotCancelableError_v0_3(String message) {
        this(null, message, null);
    }

}
