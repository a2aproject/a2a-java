package org.a2aproject.sdk.compat03.spec;

/**
 * Exception to indicate a general failure related to an A2A server.
 */
public class A2AServerException_v0_3 extends A2AException_v0_3 {

    public A2AServerException_v0_3() {
        super();
    }

    public A2AServerException_v0_3(final String msg) {
        super(msg);
    }

    public A2AServerException_v0_3(final Throwable cause) {
        super(cause);
    }

    public A2AServerException_v0_3(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
