package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.TASK_NOT_FOUND_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;

/**
 * A2A Protocol error indicating that the requested task ID does not exist.
 * <p>
 * This error is returned when a client attempts to perform operations on a task using
 * a task ID that is not found in the server's task store.
 * <p>
 * Common causes:
 * <ul>
 *   <li>Task ID was never created</li>
 *   <li>Task has been removed from the task store (expired or deleted)</li>
 *   <li>Task ID typo or incorrect value</li>
 *   <li>Task belongs to a different agent or server instance</li>
 * </ul>
 * <p>
 * Corresponds to A2A-specific error code {@code -32001}.
 * <p>
 * Usage example:
 * <pre>{@code
 * Task task = taskStore.getTask(taskId);
 * if (task == null) {
 *     throw new TaskNotFoundError();
 * }
 * }</pre>
 *
 * @see Task for task object definition
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class TaskNotFoundError extends A2AProtocolError {

    /**
     * Constructs error with default message.
     */
    public TaskNotFoundError() {
        this(null, null, null);
    }

    /**
     * Constructs error with all parameters.
     *
     * @param code the error code (defaults to -32001 if null)
     * @param message the error message (defaults to "Task not found" if null)
     * @param data additional error data (optional)
     */
    public TaskNotFoundError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, TASK_NOT_FOUND_ERROR_CODE),
                defaultIfNull(message, "Task not found"),
                data,
                "https://a2a-protocol.org/errors/task-not-found");
    }
}
