package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.JSON_PARSE_ERROR_CODE;
import static io.a2a.util.Utils.defaultIfNull;

import org.jspecify.annotations.Nullable;

/**
 * JSON-RPC error indicating that the server received invalid JSON that could not be parsed.
 * <p>
 * This error is returned when the request payload is not valid JSON, such as malformed syntax,
 * unexpected tokens, or encoding issues. This is distinct from {@link InvalidRequestError},
 * which indicates structurally valid JSON that doesn't conform to the JSON-RPC specification.
 * <p>
 * Corresponds to JSON-RPC 2.0 error code {@code -32700}.
 * <p>
 * Usage example:
 * <pre>{@code
 * try {
 *     objectMapper.readValue(payload, JSONRPCRequest.class);
 * } catch (io.a2a.json.JsonProcessingException e) {
 *     throw new JSONParseError("Malformed JSON: " + e.getMessage());
 * }
 * }</pre>
 *
 * @see A2AError for the base error class
 * @see A2AError for the error marker interface
 * @see InvalidRequestError for structurally valid but invalid requests
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class JSONParseError extends A2AError {

    /**
     * Constructs a JSON parse error with default message.
     */
    public JSONParseError() {
        this(null, null, null);
    }

    /**
     * Constructs a JSON parse error with a message.
     *
     * @param message the error message
     */
    public JSONParseError(String message) {
        this(null, message, null);
    }

    /**
     * Constructs a JSON parse error with full parameters.
     *
     * @param code the error code
     * @param message the error message
     * @param data additional error data
     */
    public JSONParseError(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
        super(
                defaultIfNull(code, JSON_PARSE_ERROR_CODE),
                defaultIfNull(message, "Invalid JSON payload"),
                data);
    }
}
