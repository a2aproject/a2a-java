package io.a2a.spec;

/**
 * Exception to indicate a general failure related to an A2A server.
 */
public class A2AServerException extends A2AException {

    /**
     * Constructs a new A2AServerException with no detail message.
     */
    public A2AServerException() {
        super();
    }

    /**
     * Constructs a new A2AServerException with the specified detail message.
     *
     * @param msg the detail message explaining the server error
     */
    public A2AServerException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new A2AServerException with the specified cause.
     *
     * @param cause the underlying cause of this server exception
     */
    public A2AServerException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new A2AServerException with the specified detail message and cause.
     *
     * @param msg the detail message explaining the server error
     * @param cause the underlying cause of this server exception
     */
    public A2AServerException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
