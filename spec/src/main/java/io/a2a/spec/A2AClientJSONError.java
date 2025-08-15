package io.a2a.spec;

/**
 * Exception class representing JSON-specific errors that occur in A2A client operations.
 * <p>
 * This exception extends {@link A2AClientError} and is thrown when JSON parsing,
 * serialization, or deserialization errors occur during A2A protocol interactions.
 * Common scenarios include malformed JSON responses, invalid JSON structure,
 * or JSON schema validation failures.
 * </p>
 */
public class A2AClientJSONError extends A2AClientError {

    /**
     * Constructs a new A2AClientJSONError with no detail message.
     */
    public A2AClientJSONError() {
    }

    /**
     * Constructs a new A2AClientJSONError with the specified detail message.
     *
     * @param message the detail message explaining the JSON error
     */
    public A2AClientJSONError(String message) {
        super(message);
    }

    /**
     * Constructs a new A2AClientJSONError with the specified detail message and cause.
     *
     * @param message the detail message explaining the JSON error
     * @param cause the underlying cause of this JSON error
     */
    public A2AClientJSONError(String message, Throwable cause) {
        super(message, cause);
    }
}
