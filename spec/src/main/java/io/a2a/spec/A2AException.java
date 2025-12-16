package io.a2a.spec;

/**
 * Base exception for A2A Protocol-related failures.
 * <p>
 * This is the root exception class for all A2A Protocol exceptions, providing a common
 * base type for exception handling. It extends {@link RuntimeException} to allow unchecked
 * exception propagation throughout A2A client and server implementations.
 * <p>
 * Specialized subclasses:
 * <ul>
 *   <li>{@link A2AServerException} - Server-side failures</li>
 *   <li>{@link A2AClientException} - Client-side failures</li>
 * </ul>
 * @see A2AServerException for server-side errors
 * @see A2AClientException for client-side errors
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
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
