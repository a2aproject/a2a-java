package org.a2aproject.sdk.compat03.spec;

public class A2AClientInvalidArgsError_v0_3 extends A2AClientError_v0_3 {

    public A2AClientInvalidArgsError_v0_3() {
    }

    public A2AClientInvalidArgsError_v0_3(String message) {
        super("Invalid arguments error: " + message);
    }

    public A2AClientInvalidArgsError_v0_3(String message, Throwable cause) {
        super("Invalid arguments error: " + message, cause);
    }
}
