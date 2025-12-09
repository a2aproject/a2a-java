package io.a2a.json;

import org.jspecify.annotations.Nullable;

/**
 * General exception for JSON processing errors during serialization or deserialization.
 * <p>
 * This exception serves as a replacement for Jackson's JsonProcessingException, allowing
 * the A2A Java SDK to remain independent of any specific JSON library implementation.
 * It can be used with any JSON processing library (Gson, Jackson, etc.).
 * <p>
 * This is the base class for more specific JSON processing exceptions like
 * {@link JsonMappingException}.
 * <p>
 * Usage example:
 * <pre>{@code
 * try {
 *     String json = gson.toJson(object);
 * } catch (Exception e) {
 *     throw new JsonProcessingException("Failed to serialize object", e);
 * }
 * }</pre>
 *
 * @see JsonMappingException for mapping-specific errors
 */
public class JsonProcessingException extends Exception {

    /**
     * Constructs a new JsonProcessingException with the specified message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public JsonProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new JsonProcessingException with the specified message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of the exception (may be null)
     */
    public JsonProcessingException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new JsonProcessingException with the specified cause.
     *
     * @param cause the underlying cause of the exception
     */
    public JsonProcessingException(Throwable cause) {
        super(cause);
    }
}
