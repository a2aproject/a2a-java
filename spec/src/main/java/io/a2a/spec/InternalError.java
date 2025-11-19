package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32603;

    @JsonCreator
    public InternalError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Internal Error"),
                data);
    }

    public InternalError(String message) {
        this(null, message, null);
    }
}
