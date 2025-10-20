package io.a2a.spec;

/**
 * Base exception for A2A Client errors.
 */
public class A2AClientError extends RuntimeException {
    public A2AClientError() {
    }

    public A2AClientError(String message) {
        super(message);
    }

    public A2AClientError(String message, Throwable cause) {
        super(message, cause);
    }
}
