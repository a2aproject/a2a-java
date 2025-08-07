package io.a2a.server.events;

public class TaskQueueExistsException extends RuntimeException {
    public TaskQueueExistsException() {
    }

    public TaskQueueExistsException(String message) {
        super(message);
    }

    public TaskQueueExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskQueueExistsException(Throwable cause) {
        super(cause);
    }

    public TaskQueueExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
