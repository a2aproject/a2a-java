package io.a2a.spec;

/**
 * Exception indicating a client-side failure in A2A Protocol operations.
 * <p>
 * This exception is thrown by A2A client implementations when encountering errors
 * during communication with agents, response validation, or client-side processing.
 * <p>
 * Common scenarios:
 * <ul>
 *   <li>Network communication failures</li>
 *   <li>Invalid agent responses ({@link A2AClientError})</li>
 *   <li>HTTP errors ({@link A2AClientHTTPError})</li>
 *   <li>JSON parsing errors ({@link A2AClientJSONError})</li>
 * </ul>
 * @see A2AException for the base exception class
 * @see A2AServerException for server-side errors
 * @see A2AClientError for more specific client errors
 */
public class A2AClientException extends A2AException {

    /**
     * Constructs a new A2AClientException with no detail message or cause.
     */
    public A2AClientException() {
        super();
    }

    /**
     * Constructs a new A2AClientException with the specified detail message.
     *
     * @param msg the detail message
     */
    public A2AClientException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new A2AClientException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public A2AClientException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new A2AClientException with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause of this exception
     */
    public A2AClientException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
