package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC error indicating that the request payload is not a valid JSON-RPC Request object.
 * <p>
 * This error is returned when the JSON-RPC request fails structural validation.
 * Common causes include:
 * <ul>
 *   <li>Missing required JSON-RPC fields (jsonrpc, method, id)</li>
 *   <li>Invalid JSON-RPC version (must be "2.0")</li>
 *   <li>Malformed request structure</li>
 *   <li>Type mismatches in required fields</li>
 * </ul>
 * <p>
 * Corresponds to JSON-RPC 2.0 error code {@code -32600}.
 * <p>
 * Usage example:
 * <pre>{@code
 * // Default error with standard message
 * throw new InvalidRequestError();
 *
 * // Custom error message
 * throw new InvalidRequestError("Missing 'method' field in request");
 * }</pre>
 *
 * @see JSONRPCError for the base error class
 * @see A2AError for the error marker interface
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidRequestError extends JSONRPCError {

    public final static Integer DEFAULT_CODE = -32600;

    public InvalidRequestError() {
        this(null, null, null);
    }

    @JsonCreator
    public InvalidRequestError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Request payload validation error"),
                data);
    }

    public InvalidRequestError(String message) {
        this(null, message, null);
    }
}
