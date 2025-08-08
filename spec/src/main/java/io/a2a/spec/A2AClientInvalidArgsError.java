package io.a2a.spec;

public class A2AClientInvalidArgsError extends A2AClientError {

    public A2AClientInvalidArgsError() {
    }

    public A2AClientInvalidArgsError(String message) {
        super("Invalid arguments error: " + message);
    }

    public A2AClientInvalidArgsError(String message, Throwable cause) {
        super("Invalid arguments error: " + message, cause);
    }
}
