package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a JSON-RPC error that occurs when a requested method is not found.
 * This error is part of the A2A protocol's error handling mechanism and follows
 * the JSON-RPC 2.0 specification for method not found errors.
 *
 * <p>This error is typically returned when:</p>
 * <ul>
 *   <li>The requested method name does not exist on the server</li>
 *   <li>The method is not available in the current context</li>
 *   <li>The method has been deprecated or removed</li>
 * </ul>
 *
 * <p>According to JSON-RPC 2.0, this error uses code -32601.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MethodNotFoundError extends JSONRPCError {

    /** The default JSON-RPC error code for method not found errors */
    public final static Integer DEFAULT_CODE = -32601;

    /**
     * Constructs a new MethodNotFoundError with the specified error details.
     * This constructor is used by Jackson for JSON deserialization.
     *
     * @param code the JSON-RPC error code (defaults to -32601 if null)
     * @param message a human-readable error message (defaults to "Method not found" if null)
     * @param data additional error data providing context (can be null)
     */
    @JsonCreator
    public MethodNotFoundError(
            @JsonProperty("code") Integer code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Method not found"),
                data);
    }

    /**
     * Constructs a new MethodNotFoundError with default values.
     * Uses the default error code (-32601) and message ("Method not found").
     */
    public MethodNotFoundError() {
        this(DEFAULT_CODE, null, null);
    }
}
