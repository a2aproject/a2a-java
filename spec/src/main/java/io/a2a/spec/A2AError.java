package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Marker interface for A2A Protocol error events.
 * <p>
 * This interface extends {@link Event} to allow errors to be transmitted as events
 * in the A2A Protocol's event stream. All protocol-level errors implement this interface,
 * enabling uniform error handling across both streaming and non-streaming communication.
 * <p>
 * A2A errors typically extend {@link A2AError} to provide JSON-RPC 2.0 compliant
 * error responses with standard error codes, messages, and optional additional data.
 * <p>
 * Common implementations include:
 * <ul>
 *   <li>{@link InvalidParamsError} - Invalid method parameters</li>
 *   <li>{@link InvalidRequestError} - Malformed request</li>
 *   <li>{@link MethodNotFoundError} - Unknown method</li>
 *   <li>{@link InternalError} - Server-side error</li>
 *   <li>{@link TaskNotFoundError} - Task does not exist</li>
 *   <li>And others for specific protocol error conditions</li>
 * </ul>
 *
 * @see Event for the base event interface
 * @see A2AError for the base error implementation
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public class A2AError extends RuntimeException implements Event {
    /**
     * The numeric error code (see JSON-RPC 2.0 spec for standard codes).
     */
    private final Integer code;

    /**
     * Additional error details as key-value pairs.
     */
    private final Map<String, Object> details;

    /**
     * Constructs a JSON-RPC error with the specified code, message, and optional details.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     *
     * @param code the numeric error code (required, see JSON-RPC 2.0 spec for standard codes)
     * @param message the human-readable error message (required)
     * @param details additional error details as key-value pairs (defaults to empty map if null)
     * @throws IllegalArgumentException if code or message is null
     */
    public A2AError(Integer code, String message, @Nullable Map<String, Object> details) {
        super(Assert.checkNotNullParam("message", message));
        this.code = Assert.checkNotNullParam("code", code);
        this.details = details == null ? Map.of() : Map.copyOf(details);
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
     * Gets additional details about the error as key-value pairs.
     *
     * @return the error details, never null (empty map if no details provided)
     */
    public Map<String, Object> getDetails() {
        return details;
    }
}
