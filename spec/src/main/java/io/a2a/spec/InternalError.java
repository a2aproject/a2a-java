package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.INTERNAL_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;


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
 * @see JSONRPCError for the base error class
 * @see A2AError for the error marker interface
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class InternalError extends JSONRPCError {

    public InternalError(Integer code, String message, Object data) {
        super(
                defaultIfNull(code, INTERNAL_ERROR_CODE),
                defaultIfNull(message, "Internal Error"),
                data);
    }

    public InternalError(String message) {
        this(null, message, null);
    }
}
