package io.a2a.spec;

/**
 * Exception to indicate a general failure related to the A2A protocol.
 */
public class A2AException extends RuntimeException {

    /**
     * Constructs a new {@code A2AException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public A2AException() {
    }

    /**
     * Constructs a new {@code A2AException} instance with an initial message. No cause is specified.
     *
     * @param msg the message
     */
    public A2AException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code A2AException} instance with an initial cause. If a non-{@code null} cause
     * is specified, its message is used to initialize the message of this {@code A2AException}; otherwise
     * the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public A2AException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code A2AException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public A2AException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
