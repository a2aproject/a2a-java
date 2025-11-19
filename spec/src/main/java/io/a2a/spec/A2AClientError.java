package io.a2a.spec;

/**
 * Base exception for A2A client-specific error conditions.
 * <p>
 * This is a specialized exception hierarchy for client-side errors, distinct from
 * {@link A2AClientException}. It is used for errors that occur during client SDK
 * operations such as validation, state management, and protocol handling.
 * <p>
 * Specialized subclasses:
 * <ul>
 *   <li>{@link A2AClientHTTPError} - HTTP transport errors with status codes</li>
 *   <li>{@link A2AClientJSONError} - JSON serialization/deserialization errors</li>
 *   <li>{@link A2AClientInvalidStateError} - Invalid client state errors</li>
 *   <li>{@link A2AClientInvalidArgsError} - Invalid argument errors</li>
 * </ul>
 *
 * @see A2AClientException for general client exceptions
 * @see A2AClientHTTPError for HTTP-specific errors
 * @see A2AClientJSONError for JSON-specific errors
 */
public class A2AClientError extends RuntimeException {
    /**
     * Constructs a new A2AClientError with no detail message.
     */
    public A2AClientError() {
    }

    /**
     * Constructs a new A2AClientError with the specified detail message.
     *
     * @param message the detail message
     */
    public A2AClientError(String message) {
        super(message);
    }

    /**
     * Constructs a new A2AClientError with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public A2AClientError(String message, Throwable cause) {
        super(message, cause);
    }
}
