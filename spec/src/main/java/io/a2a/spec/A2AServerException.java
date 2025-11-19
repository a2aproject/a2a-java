package io.a2a.spec;

/**
 * Exception indicating a server-side failure in A2A Protocol operations.
 * <p>
 * This exception is thrown by A2A server implementations when encountering errors
 * during request processing, task execution, or other server-side operations.
 * <p>
 * Common scenarios:
 * <ul>
 *   <li>Agent execution failures</li>
 *   <li>Task store persistence errors</li>
 *   <li>Resource exhaustion or limits exceeded</li>
 *   <li>Configuration errors</li>
 * </ul>
 *
 * @see A2AException for the base exception class
 * @see A2AClientException for client-side errors
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
