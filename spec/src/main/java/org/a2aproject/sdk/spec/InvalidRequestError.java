package org.a2aproject.sdk.spec;

import static org.a2aproject.sdk.util.Utils.defaultIfNull;

import java.util.Map;

import org.jspecify.annotations.Nullable;


/**
 * JSON-RPC error indicating that the request payload is not a valid JSON-RPC Request object.
 * <p>
 * This error is returned when the JSON-RPC request fails structural validation.
 * Common causes include:
 * <ul>
 * <li>Missing required JSON-RPC fields (jsonrpc, method, id)</li>
 * <li>Invalid JSON-RPC version (must be "2.0")</li>
 * <li>Malformed request structure</li>
 * <li>Type mismatches in required fields</li>
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
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class InvalidRequestError extends A2AError {

    /**
     * Constructs an invalid request error with default message.
     */
    public InvalidRequestError() {
        this(null, null, null);
    }

    /**
     * Constructs an invalid request error with full parameters.
     *
     * @param code the error code
     * @param message the error message
     * @param details additional error details
     */
    public InvalidRequestError(@Nullable Integer code, @Nullable String message, @Nullable Map<String, Object> details) {
        super(
                defaultIfNull(code, A2AErrorCodes.INVALID_REQUEST.code()),
                defaultIfNull(message, "Request payload validation error"),
                details);
    }

    /**
     * Constructs an invalid request error with a message.
     *
     * @param message the error message
     */
    public InvalidRequestError(String message) {
        this(null, message, null);
    }
}
