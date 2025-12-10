package io.a2a.spec;

/**
 * Client exception indicating an invalid state for the requested operation.
 * <p>
 * This exception is thrown when a client operation is attempted while the client
 * is in a state that doesn't permit that operation. This ensures proper sequencing
 * and lifecycle management of client operations.
 * <p>
 * Common scenarios:
 * <ul>
 *   <li>Attempting to send messages before client initialization</li>
 *   <li>Trying to cancel a task after the client connection is closed</li>
 *   <li>Performing operations on a client that has been shut down</li>
 *   <li>Violating client state machine transitions</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * if (!client.isConnected()) {
 *     throw new A2AClientInvalidStateError("Client not connected");
 * }
 * }</pre>
 *
 * @see A2AClientError for the base client error class
 */
public class A2AClientInvalidStateError extends A2AClientError {

    /**
     * Creates a new invalid state error with no message.
     */
    public A2AClientInvalidStateError() {
    }

    /**
     * Creates a new invalid state error with the specified message.
     *
     * @param message the error message
     */
    public A2AClientInvalidStateError(String message) {
        super("Invalid state error: " + message);
    }

    /**
     * Creates a new invalid state error with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public A2AClientInvalidStateError(String message, Throwable cause) {
        super("Invalid state error: " + message, cause);
    }
}
