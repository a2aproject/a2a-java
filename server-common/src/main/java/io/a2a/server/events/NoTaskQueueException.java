package io.a2a.server.events;

/**
 * Exception thrown when attempting to access a task queue that does not exist.
 * <p>
 * This exception is typically thrown when trying to retrieve or operate on
 * an event queue for a task that has not been created or has been removed.
 * </p>
 */
public class NoTaskQueueException extends RuntimeException {
    /**
     * Creates a NoTaskQueueException with no message.
     */
    public NoTaskQueueException() {
    }

    /**
     * Creates a NoTaskQueueException with the specified detail message.
     *
     * @param message the detail message
     */
    public NoTaskQueueException(String message) {
        super(message);
    }

    /**
     * Creates a NoTaskQueueException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public NoTaskQueueException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a NoTaskQueueException with the specified cause.
     *
     * @param cause the cause
     */
    public NoTaskQueueException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a NoTaskQueueException with the specified message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     *
     * @param message the detail message
     * @param cause the cause
     * @param enableSuppression whether suppression is enabled or disabled
     * @param writableStackTrace whether the stack trace should be writable
     */
    public NoTaskQueueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
