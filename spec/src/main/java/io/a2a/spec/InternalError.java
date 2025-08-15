package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 Internal Error.
 * 
 * This error indicates that an internal JSON-RPC error occurred on the server.
 * This is a standard JSON-RPC error code as defined in the specification.
 * The error signifies that the server encountered an unexpected condition
 * that prevented it from fulfilling the request.
 * 
 * Error code: -32603
 * Default message: "Internal Error"
 * 
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalError extends JSONRPCError {

    /** The standard JSON-RPC error code for internal errors */
    public final static Integer DEFAULT_CODE = -32603;

    /**
     * Full constructor for InternalError.
     * Used by Jackson for JSON deserialization.
     * 
     * @param code the error code (defaults to -32603 if null)
     * @param message the error message (defaults to "Internal Error" if null)
     * @param data additional error data (optional)
     */
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

    /**
     * Convenience constructor with custom message.
     * Uses the default error code (-32603) and no additional data.
     * 
     * @param message the custom error message
     */
    public InternalError(String message) {
        this(null, message, null);
    }
}
