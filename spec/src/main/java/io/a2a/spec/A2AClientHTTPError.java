package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Exception class representing HTTP-specific errors that occur in A2A client operations.
 * <p>
 * This exception extends {@link A2AClientError} and provides additional information
 * about HTTP-related failures, including the HTTP status code and error message.
 * It is thrown when HTTP communication errors occur during A2A protocol interactions.
 * </p>
 */
public class A2AClientHTTPError extends A2AClientError {
    private final int code;
    private final String message;

    /**
     * Constructs a new A2AClientHTTPError with the specified HTTP status code, message, and data.
     *
     * @param code the HTTP status code associated with this error
     * @param message the error message describing the HTTP error
     * @param data additional data associated with the error (currently unused but reserved for future use)
     * @throws IllegalArgumentException if message is null
     */
    public A2AClientHTTPError(int code, String message, Object data) {
        Assert.checkNotNullParam("code", code);
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.message = message;
    }

    /**
     * Gets the error code
     *
     * @return the error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the error message
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }
}
