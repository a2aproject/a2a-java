package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC 2.0 Invalid Params Error.
 * 
 * This error indicates that the method parameters are invalid. This is a standard
 * JSON-RPC error code used when the server receives a request with invalid parameter
 * values, missing required parameters, or parameters that don't match the expected
 * format or type for the requested method.
 * 
 * Error code: -32602
 * Default message: "Invalid parameters"
 * 
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidParamsError extends JSONRPCError {

    /** The standard JSON-RPC error code for invalid parameters */
    public final static Integer DEFAULT_CODE = -32602;

    /**
     * Full constructor for InvalidParamsError.
     * Used by Jackson for JSON deserialization.
     * 
     * @param code the error code (defaults to -32602 if null)
     * @param message the error message (defaults to "Invalid parameters" if null)
     * @param data additional error data (optional)
     */
    @JsonCreator
    public InvalidParamsError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Invalid parameters"),
                data);
    }

    /**
     * Convenience constructor with custom message.
     * Uses the default error code (-32602) and no additional data.
     * 
     * @param message the custom error message
     */
    public InvalidParamsError(String message) {
        this(null, message, null);
    }

    public InvalidParamsError() {
        this(null, null, null);
    }
}
