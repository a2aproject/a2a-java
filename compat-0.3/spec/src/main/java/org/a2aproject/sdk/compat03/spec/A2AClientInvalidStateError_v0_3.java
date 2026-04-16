package org.a2aproject.sdk.compat03.spec;

public class A2AClientInvalidStateError_v0_3 extends A2AClientError_v0_3 {

    public A2AClientInvalidStateError_v0_3() {
    }

    public A2AClientInvalidStateError_v0_3(String message) {
        super("Invalid state error: " + message);
    }

    public A2AClientInvalidStateError_v0_3(String message, Throwable cause) {
        super("Invalid state error: " + message, cause);
    }
}
