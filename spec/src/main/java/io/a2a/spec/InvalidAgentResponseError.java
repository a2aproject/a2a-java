package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A2A-specific error indicating that an agent returned an invalid response for the current method.
 * 
 * This error is used when an A2A agent returns a response that doesn't conform to the
 * expected format or specification for the requested method. This could include:
 * - Missing required fields in the response
 * - Invalid data types or formats
 * - Response structure that doesn't match the A2A specification
 * - Malformed JSON or other parsing issues
 * 
 * Error code: -32006 (A2A-specific extension)
 * Default message: "Invalid agent response"
 * 
 * This is an extension to the standard JSON-RPC error codes for A2A protocol-specific errors.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvalidAgentResponseError extends JSONRPCError {

    /** The A2A-specific error code for invalid agent responses */
    public final static Integer DEFAULT_CODE = -32006;

    /**
     * Full constructor for InvalidAgentResponseError.
     * Used by Jackson for JSON deserialization.
     * 
     * @param code the error code (defaults to -32006 if null)
     * @param message the error message (defaults to "Invalid agent response" if null)
     * @param data additional error data (optional)
     */
    @JsonCreator
    public InvalidAgentResponseError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Invalid agent response"),
                data);
    }
}
