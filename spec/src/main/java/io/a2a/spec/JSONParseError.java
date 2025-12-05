package io.a2a.spec;

import static io.a2a.spec.A2AErrorCodes.JSON_PARSE_ERROR_CODE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static io.a2a.util.Utils.defaultIfNull;

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
 * } catch (JsonProcessingException e) {
 *     throw new JSONParseError("Malformed JSON: " + e.getMessage());
 * }
 * }</pre>
 *
 * @see JSONRPCError for the base error class
 * @see A2AError for the error marker interface
 * @see InvalidRequestError for structurally valid but invalid requests
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JSONParseError extends JSONRPCError implements A2AError {

    public JSONParseError() {
        this(null, null, null);
    }

    public JSONParseError(String message) {
        this(null, message, null);
    }

    @JsonCreator
    public JSONParseError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, JSON_PARSE_ERROR_CODE),
                defaultIfNull(message, "Invalid JSON payload"),
                data);
    }
}
