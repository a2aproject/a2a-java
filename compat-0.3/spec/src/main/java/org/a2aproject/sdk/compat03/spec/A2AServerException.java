package org.a2aproject.sdk.compat03.spec;

/**
 * Exception to indicate a general failure related to an A2A server.
 */
public class A2AServerException extends A2AException {

    public A2AServerException() {
        super();
    }

    public A2AServerException(final String msg) {
        super(msg);
    }

    public A2AServerException(final Throwable cause) {
        super(cause);
    }

    public A2AServerException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
