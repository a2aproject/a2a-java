package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;

/**
 * JSON-RPC error indicating that the requested method does not exist or is not available.
 * <p>
 * This error is returned when a client attempts to invoke a JSON-RPC method that is not
 * implemented by the agent. In the A2A Protocol context, this typically means calling
 * an unsupported protocol method.
 * <p>
 * Corresponds to JSON-RPC 2.0 error code {@code -32601}.
 * <p>
 * Usage example:
 * <pre>{@code
 * // Standard error for unknown method
 * throw new MethodNotFoundError();
 * }</pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class MethodNotFoundError extends A2AError {

    /**
     * Constructs error with all parameters.
     *
     * @param code the error code (defaults to -32601 if null)
     * @param message the error message (defaults to "Method not found" if null)
     * @param data additional error data (optional)
     */
    public MethodNotFoundError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, METHOD_NOT_FOUND_ERROR_CODE),
                defaultIfNull(message, "Method not found"),
                data);
    }

    /**
     * Constructs error with default message.
     */
    public MethodNotFoundError() {
        this(METHOD_NOT_FOUND_ERROR_CODE, null, null);
    }
}
