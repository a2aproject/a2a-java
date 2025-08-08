package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 Invalid Request Error.
 * 
 * This error indicates that the JSON sent is not a valid Request object.
 * This is a standard JSON-RPC error code used when the server receives
 * a request that doesn't conform to the JSON-RPC specification format.
 * Common causes include:
 * - Malformed JSON syntax
 * - Missing required fields (jsonrpc, method)
 * - Invalid field types or values
 * - Request structure that doesn't match the JSON-RPC specification
 * 
 * Error code: -32600
 * Default message: "Invalid Request"
 * 
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidRequestError extends JSONRPCError {

    /** The standard JSON-RPC error code for invalid requests */
    public final static Integer DEFAULT_CODE = -32600;

    public InvalidRequestError() {
        this(null, null, null);
    }

    /**
     * Full constructor for InvalidRequestError.
     * Used by Jackson for JSON deserialization.
     * 
     * @param code the error code (defaults to -32600 if null)
     * @param message the error message (defaults to "Invalid Request" if null)
     * @param data additional error data (optional)
     */
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

    /**
     * Convenience constructor with custom message.
     * Uses the default error code (-32600) and no additional data.
     * 
     * @param message the custom error message
     */
    public InvalidRequestError(String message) {
        this(null, message, null);
    }
}
