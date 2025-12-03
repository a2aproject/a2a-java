package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MethodNotFoundError extends JSONRPCError {

    @JsonCreator
    public MethodNotFoundError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, METHOD_NOT_FOUND_ERROR_CODE),
                defaultIfNull(message, "Method not found"),
                data);
    }

    public MethodNotFoundError() {
        this(METHOD_NOT_FOUND_ERROR_CODE, null, null);
    }
}
