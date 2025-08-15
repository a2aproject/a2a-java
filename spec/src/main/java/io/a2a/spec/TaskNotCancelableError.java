package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 error indicating that a task cannot be canceled.
 * This error is returned when a client attempts to cancel a task that is not
 * in a cancelable state, such as tasks that have already completed, failed,
 * or are in a critical phase where cancellation is not permitted.
 *
 * <p>Common scenarios where this error occurs:</p>
 * <ul>
 *   <li>Task has already completed successfully</li>
 *   <li>Task has already failed or been canceled</li>
 *   <li>Task is in a critical execution phase where cancellation would cause data corruption</li>
 *   <li>Task type does not support cancellation by design</li>
 * </ul>
 *
 * <p>The error follows JSON-RPC 2.0 error object specification with a default
 * error code of -32002 and a descriptive message. Additional context can be
 * provided through the data field.</p>
 *
 * @see JSONRPCError
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskNotCancelableError extends JSONRPCError {

    /** Default error code for task not cancelable errors */
    public final static Integer DEFAULT_CODE = -32002;

    /**
     * Creates a TaskNotCancelableError with default values.
     * Uses the default error code and message.
     */
    public TaskNotCancelableError() {
        this(null, null, null);
    }

    /**
     * Creates a TaskNotCancelableError with custom values.
     * Any null parameters will be replaced with default values.
     *
     * @param code the error code (defaults to -32002 if null)
     * @param message the error message (defaults to "Task cannot be canceled" if null)
     * @param data additional error data (can be null)
     */
    @JsonCreator
    public TaskNotCancelableError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Task cannot be canceled"),
                data);
    }

}
