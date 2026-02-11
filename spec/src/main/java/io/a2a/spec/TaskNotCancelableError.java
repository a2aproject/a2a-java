package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.TASK_NOT_CANCELABLE_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;

/**
 * A2A Protocol error indicating that a task cannot be canceled in its current state.
 * <p>
 * This error is returned when a client attempts to cancel a task
 * but the task is in a terminal state ({@link TaskState#COMPLETED}, {@link TaskState#FAILED},
 * {@link TaskState#CANCELED}) where cancellation is not applicable.
 * <p>
 * Tasks can only be canceled when they are in non-terminal states such as {@link TaskState#SUBMITTED}
 * or {@link TaskState#WORKING}.
 * <p>
 * Corresponds to A2A-specific error code {@code -32002}.
 * <p>
 * Usage example:
 * <pre>{@code
 * Task task = taskStore.getTask(taskId);
 * if (task.status().state().isFinal()) {
 *     throw new TaskNotCancelableError(
 *         "Task in " + task.status().state() + " state cannot be canceled"
 *     );
 * }
 * }</pre>
 *
 * @see TaskState for task state definitions
 * @see TaskStatus#state() for current task state
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class TaskNotCancelableError extends A2AProtocolError {

    /**
     * Constructs error with default message.
     */
    public TaskNotCancelableError() {
        this(null, null, null);
    }

    /**
     * Constructs error with all parameters.
     *
     * @param code the error code (defaults to -32002 if null)
     * @param message the error message (defaults to "Task cannot be canceled" if null)
     * @param data additional error data (optional)
     */
    public TaskNotCancelableError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, TASK_NOT_CANCELABLE_ERROR_CODE),
                defaultIfNull(message, "Task cannot be canceled"),
                data,
                "https://a2a-protocol.org/errors/task-not-cancelable");
    }

    /**
     * Constructs error with custom message.
     *
     * @param message the error message
     */
    public TaskNotCancelableError(String message) {
        this(null, message, null);
    }

}
