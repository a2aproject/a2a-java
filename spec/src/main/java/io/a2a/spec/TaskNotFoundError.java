package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static io.a2a.util.Utils.defaultIfNull;

/**
 * Represents a JSON-RPC 2.0 error indicating that a requested task was not found.
 * This error is returned when a client attempts to access, modify, or query a task
 * that does not exist in the agent's task registry or has been removed.
 *
 * <p>Common scenarios where this error occurs:</p>
 * <ul>
 *   <li>Task ID does not exist in the system</li>
 *   <li>Task has been deleted or expired</li>
 *   <li>Task ID format is invalid or malformed</li>
 *   <li>Client lacks permission to access the specified task</li>
 * </ul>
 *
 * <p>The error follows JSON-RPC 2.0 error object specification with a default
 * error code of -32001 and a descriptive message. Additional context such as
 * the invalid task ID can be provided through the data field.</p>
 *
 * @see JSONRPCError
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskNotFoundError extends JSONRPCError {

    /** Default error code for task not found errors */
    public final static Integer DEFAULT_CODE = -32001;

    /**
     * Creates a TaskNotFoundError with default values.
     * Uses the default error code and message.
     */
    public TaskNotFoundError() {
        this(null, null, null);
    }

    /**
     * Creates a TaskNotFoundError with custom values.
     * Any null parameters will be replaced with default values.
     *
     * @param code the error code (defaults to -32001 if null)
     * @param message the error message (defaults to "Task not found" if null)
     * @param data additional error data, such as the invalid task ID (can be null)
     */
    @JsonCreator
    public TaskNotFoundError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Task not found"),
                data);
    }
}
