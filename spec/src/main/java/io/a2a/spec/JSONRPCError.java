package io.a2a.spec;


import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC 2.0 error object as defined in the JSON-RPC 2.0 specification.
 * <p>
 * This class encapsulates error information returned in JSON-RPC error responses.
 * According to the JSON-RPC 2.0 specification, an error object must contain:
 * <ul>
 *   <li>{@code code} - A number indicating the error type (required)</li>
 *   <li>{@code message} - A short description of the error (required)</li>
 *   <li>{@code data} - Additional information about the error (optional)</li>
 * </ul>
 * <p>
 * This class implements {@link Event} to allow errors to be streamed to clients,
 * and {@link A2AError} to integrate with the A2A Protocol's error handling system.
 * It extends {@link Error} to provide standard Java error semantics with a message.
 * <p>
 * Standard error codes are defined in the JSON-RPC 2.0 specification:
 * <ul>
 *   <li>-32700: Parse error</li>
 *   <li>-32600: Invalid Request</li>
 *   <li>-32601: Method not found</li>
 *   <li>-32602: Invalid params</li>
 *   <li>-32603: Internal error</li>
 *   <li>-32000 to -32099: Server error (implementation-defined)</li>
 * </ul>
 *
 * @see Event
 * @see A2AError
 * @see JSONRPCErrorResponse
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 */
public class JSONRPCError extends Error implements Event, A2AError {

    /**
     * The numeric error code (see JSON-RPC 2.0 spec for standard codes).
     */
    private final Integer code;

    /**
     * Additional error information (structure defined by the error code).
     */
    private final Object data;

    /**
     * Constructs a JSON-RPC error with the specified code, message, and optional data.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     *
     * @param code the numeric error code (required, see JSON-RPC 2.0 spec for standard codes)
     * @param message the human-readable error message (required)
     * @param data additional error information, structure defined by the error code (optional)
     * @throws IllegalArgumentException if code or message is null
     */
    public JSONRPCError(Integer code, String message, Object data) {
        super(message);
        Assert.checkNotNullParam("code", code);
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.data = data;
    }

    /**
     * Gets the numeric error code indicating the error type.
     * <p>
     * Standard JSON-RPC 2.0 error codes:
     * <ul>
     *   <li>-32700: Parse error</li>
     *   <li>-32600: Invalid Request</li>
     *   <li>-32601: Method not found</li>
     *   <li>-32602: Invalid params</li>
     *   <li>-32603: Internal error</li>
     *   <li>-32000 to -32099: Server error (implementation-defined)</li>
     * </ul>
     *
     * @return the error code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * Gets additional information about the error.
     * <p>
     * The structure and type of the data field is defined by the specific error code.
     * It may contain detailed debugging information, validation errors, or other
     * context-specific data to help diagnose the error.
     *
     * @return the error data, or null if not provided
     */
    public Object getData() {
        return data;
    }
}
