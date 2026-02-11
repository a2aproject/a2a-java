package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.INVALID_PARAMS_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;

/**
 * JSON-RPC error indicating that method parameters are invalid or missing required fields.
 * <p>
 * This error is returned when a JSON-RPC method is called with parameters that fail validation.
 * Common causes include:
 * <ul>
 * <li>Missing required parameters</li>
 * <li>Parameters of incorrect type</li>
 * <li>Parameter values outside acceptable ranges</li>
 * <li>Malformed parameter structures</li>
 * </ul>
 * <p>
 * Corresponds to JSON-RPC 2.0 error code {@code -32602}.
 * <p>
 * Usage example:
 * <pre>{@code
 * // Default error with standard message
 * throw new InvalidParamsError();
 *
 * // Custom error message
 * throw new InvalidParamsError("taskId parameter is required");
 * }</pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class InvalidParamsError extends A2AError {

    /**
     * Constructs an invalid params error with full parameters.
     *
     * @param code the error code
     * @param message the error message
     * @param data additional error data
     */
    public InvalidParamsError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, INVALID_PARAMS_ERROR_CODE),
                defaultIfNull(message, "Invalid parameters"),
                data);
    }

    /**
     * Constructs an invalid params error with a message.
     *
     * @param message the error message
     */
    public InvalidParamsError(@Nullable String message) {
        this(null, message, null);
    }

    /**
     * Constructs an invalid params error with default message.
     */
    public InvalidParamsError() {
        this(null, null, null);
    }
}
