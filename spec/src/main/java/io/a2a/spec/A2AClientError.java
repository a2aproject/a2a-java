package io.a2a.spec;

/**
 * Exception class representing errors that occur in A2A client operations.
 * <p>
 * This exception is thrown when client-side errors occur during A2A protocol
 * interactions, such as communication failures, invalid responses, or
 * protocol violations.
 * </p>
 */
public class A2AClientError extends Exception {
    /**
     * Constructs a new A2AClientError with no detail message.
     */
    public A2AClientError() {
    }

    /**
     * Constructs a new A2AClientError with the specified detail message.
     *
     * @param message the detail message explaining the error
     */
    public A2AClientError(String message) {
        super(message);
    }

    /**
     * Constructs a new A2AClientError with the specified detail message and cause.
     *
     * @param message the detail message explaining the error
     * @param cause the underlying cause of this exception
     */
    public A2AClientError(String message, Throwable cause) {
        super(message, cause);
    }
}
