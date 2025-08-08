package io.a2a.spec;

public class A2AClientInvalidStateError extends A2AClientError {

    public A2AClientInvalidStateError() {
    }

    public A2AClientInvalidStateError(String message) {
        super("Invalid state error: " + message);
    }

    public A2AClientInvalidStateError(String message, Throwable cause) {
        super("Invalid state error: " + message, cause);
    }
}
