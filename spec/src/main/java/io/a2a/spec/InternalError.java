package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.INTERNAL_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;


/**
 * JSON-RPC error indicating an internal error occurred on the server.
 * <p>
 * This error represents unexpected server-side failures such as unhandled exceptions,
 * resource exhaustion, or other internal issues that prevent the server from processing
 * a request. This is a catch-all error for server problems not covered by more specific
 * error types.
 * <p>
 * Corresponds to JSON-RPC 2.0 error code {@code -32603}.
 * <p>
 * Usage example:
 * <pre>{@code
 * try {
 *     // Server processing
 * } catch (Exception e) {
 *     throw new InternalError("Failed to process request: " + e.getMessage());
 * }
 * }</pre>
 *
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class InternalError extends A2AError {

    /**
     * Constructs an internal error with full parameters.
     *
     * @param code the error code
     * @param message the error message
     * @param data additional error data
     */
    public InternalError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, INTERNAL_ERROR_CODE),
                defaultIfNull(message, "Internal Error"),
                data);
    }

    /**
     * Constructs an internal error with a message.
     *
     * @param message the error message
     */
    public InternalError(@Nullable String message) {
        this(null, message, null);
    }
}
