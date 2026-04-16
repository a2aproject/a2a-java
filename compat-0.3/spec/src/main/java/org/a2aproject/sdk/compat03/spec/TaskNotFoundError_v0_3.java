package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

/**
 * An A2A-specific error indicating that the requested task ID was not found.
 */
public class TaskNotFoundError_v0_3 extends JSONRPCError_v0_3 {

    public final static Integer DEFAULT_CODE = A2AErrorCodes_v0_3.TASK_NOT_FOUND_ERROR_CODE;

    public TaskNotFoundError_v0_3() {
        this(null, null, null);
    }

    public TaskNotFoundError_v0_3(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, A2AErrorCodes_v0_3.TASK_NOT_FOUND_ERROR_CODE),
                defaultIfNull(message, "Task not found"),
                data);
    }
}
