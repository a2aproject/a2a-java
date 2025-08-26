package io.a2a.spec;

/**
 * Exception to indicate a general failure related to an A2A client.
 */
public class A2AClientException extends A2AException {

    public A2AClientException() {
        super();
    }

    public A2AClientException(final String msg) {
        super(msg);
    }

    public A2AClientException(final Throwable cause) {
        super(cause);
    }

    public A2AClientException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
