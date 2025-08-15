package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON-RPC error indicating that the requested content type is not supported by the agent.
 * This error is returned when a client requests input or output in a MIME type that the agent
 * cannot handle or process. The error code follows the JSON-RPC 2.0 specification for
 * application-specific errors.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentTypeNotSupportedError extends JSONRPCError {

    /** The default error code for content type not supported errors (-32005). */
    public final static Integer DEFAULT_CODE = -32005;

    /**
     * Creates a new ContentTypeNotSupportedError with the specified parameters.
     * 
     * @param code the error code (defaults to -32005 if null)
     * @param message the error message (defaults to "Incompatible content types" if null)
     * @param data additional error data (optional)
     */
    @JsonCreator
    public ContentTypeNotSupportedError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Incompatible content types"),
                data);
    }
}
