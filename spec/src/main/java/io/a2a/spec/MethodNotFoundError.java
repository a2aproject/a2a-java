package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

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
 * @see JSONRPCError for the base error class
 * @see A2AError for the error marker interface
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class MethodNotFoundError extends JSONRPCError {

    public MethodNotFoundError(
            Integer code,
            String message,
            Object data) {
        super(
                defaultIfNull(code, METHOD_NOT_FOUND_ERROR_CODE),
                defaultIfNull(message, "Method not found"),
                data);
    }

    public MethodNotFoundError() {
        this(METHOD_NOT_FOUND_ERROR_CODE, null, null);
    }
}
