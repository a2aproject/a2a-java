package io.a2a.json;

import org.jspecify.annotations.Nullable;

/**
 * Exception for JSON mapping errors when converting between JSON and Java objects.
 * <p>
 * This exception serves as a replacement for Jackson's JsonMappingException, allowing
 * the A2A Java SDK to remain independent of any specific JSON library implementation.
 * It represents errors that occur during the mapping phase of deserialization or
 * serialization, such as type mismatches, invalid values, or constraint violations.
 * <p>
 * This exception extends {@link JsonProcessingException} and is used for more specific
 * mapping-related errors compared to general parsing errors.
 * <p>
 * Usage example:
 * <pre>{@code
 * try {
 *     Task task = JsonUtil.fromJson(json, Task.class);
 *     if (task.id() == null) {
 *         throw new JsonMappingException(null, "Task ID cannot be null");
 *     }
 * } catch (JsonProcessingException e) {
 *     throw new JsonMappingException(null, "Invalid task format: " + e.getMessage(), e);
 * }
 * }</pre>
 *
 * @see JsonProcessingException for the base exception class
 */
public class JsonMappingException extends JsonProcessingException {

    /**
     * Optional reference object that caused the mapping error (e.g., JsonParser or field path).
     */
    private final @Nullable Object reference;

    /**
     * Constructs a new JsonMappingException with the specified reference and message.
     * <p>
     * The reference parameter can be used to provide context about where the mapping
     * error occurred (e.g., a field name, path, or parser reference). It can be null
     * if no specific reference is available.
     *
     * @param reference optional reference object providing context for the error (may be null)
     * @param message the detail message explaining the mapping error
     */
    public JsonMappingException(@Nullable Object reference, String message) {
        super(message);
        this.reference = reference;
    }

    /**
     * Constructs a new JsonMappingException with the specified reference, message, and cause.
     * <p>
     * The reference parameter can be used to provide context about where the mapping
     * error occurred (e.g., a field name, path, or parser reference). It can be null
     * if no specific reference is available.
     *
     * @param reference optional reference object providing context for the error (may be null)
     * @param message the detail message explaining the mapping error
     * @param cause the underlying cause of the mapping error (may be null)
     */
    public JsonMappingException(@Nullable Object reference, String message, @Nullable Throwable cause) {
        super(message, cause);
        this.reference = reference;
    }

    /**
     * Constructs a new JsonMappingException with the specified message and cause.
     * <p>
     * This constructor is provided for compatibility when no reference object is needed.
     *
     * @param message the detail message explaining the mapping error
     * @param cause the underlying cause of the mapping error (may be null)
     */
    public JsonMappingException(String message, @Nullable Throwable cause) {
        this(null, message, cause);
    }

    /**
     * Constructs a new JsonMappingException with the specified message.
     * <p>
     * This constructor is provided for compatibility when no reference object is needed.
     *
     * @param message the detail message explaining the mapping error
     */
    public JsonMappingException(String message) {
        this(null, message);
    }

    /**
     * Returns the reference object that provides context for the mapping error.
     * <p>
     * This may be null if no specific reference was available when the exception
     * was created.
     *
     * @return the reference object, or null if not available
     */
    public @Nullable Object getReference() {
        return reference;
    }
}
