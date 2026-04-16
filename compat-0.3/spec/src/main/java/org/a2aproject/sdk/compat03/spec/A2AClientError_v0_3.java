package org.a2aproject.sdk.compat03.spec;

/**
 * Base exception for A2A Client errors.
 */
public class A2AClientError_v0_3 extends RuntimeException {
    public A2AClientError_v0_3() {
    }

    public A2AClientError_v0_3(String message) {
        super(message);
    }

    public A2AClientError_v0_3(String message, Throwable cause) {
        super(message, cause);
    }
}
