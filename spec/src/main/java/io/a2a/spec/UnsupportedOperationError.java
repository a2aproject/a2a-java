package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 error indicating that a requested operation is not supported.
 * This error is returned when a client attempts to invoke a method or perform an action
 * that is not implemented or available in the current agent configuration.
 *
 * <p>Common scenarios where this error occurs:</p>
 * <ul>
 *   <li>Calling a method that is not implemented by the agent</li>
 *   <li>Requesting features that are disabled in the current configuration</li>
 *   <li>Using deprecated methods that are no longer supported</li>
 *   <li>Attempting operations that require capabilities not available in this agent version</li>
 * </ul>
 *
 * <p>The error follows JSON-RPC 2.0 error object specification with a default
 * error code of -32004 and a descriptive message. Additional context about
 * the unsupported operation can be provided through the data field.</p>
 *
 * @see JSONRPCError
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnsupportedOperationError extends JSONRPCError {

    /** Default error code for unsupported operation errors */
    public final static Integer DEFAULT_CODE = -32004;

    /**
     * Creates an UnsupportedOperationError with custom values.
     * Any null parameters will be replaced with default values.
     *
     * @param code the error code (defaults to -32004 if null)
     * @param message the error message (defaults to "This operation is not supported" if null)
     * @param data additional error data, such as the unsupported method name (can be null)
     */
    @JsonCreator
    public UnsupportedOperationError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "This operation is not supported"),
                data);
    }

    /**
     * Creates an UnsupportedOperationError with default values.
     * Uses the default error code and message.
     */
    public UnsupportedOperationError() {
        this(null, null, null);
    }
}
