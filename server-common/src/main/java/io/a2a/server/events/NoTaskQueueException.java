package io.a2a.server.events;

public class NoTaskQueueException extends RuntimeException {
    public NoTaskQueueException() {
    }

    public NoTaskQueueException(String message) {
        super(message);
    }

    public NoTaskQueueException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoTaskQueueException(Throwable cause) {
        super(cause);
    }

    public NoTaskQueueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
