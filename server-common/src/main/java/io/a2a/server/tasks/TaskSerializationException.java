package io.a2a.server.tasks;

import org.jspecify.annotations.Nullable;

/**
 * Exception for task serialization/deserialization failures.
 * <p>
 * Indicates failures converting between Task domain objects and persistent storage format (JSON).
 * These failures are typically <b>non-transient</b> - they indicate data corruption, schema
 * mismatches, or invalid field values that require manual intervention.
 *
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>JSON parsing errors during {@code get()} operations</li>
 *   <li>JSON serialization errors during {@code save()} operations</li>
 *   <li>Invalid enum values or missing required fields</li>
 *   <li>Data format version mismatches after upgrades</li>
 * </ul>
 *
 * <h2>Recovery Strategy</h2>
 * <b>Non-Transient</b>: Retry will not help. Requires:
 * <ul>
 *   <li>Data migration or repair for corrupted tasks</li>
 *   <li>Schema updates for version mismatches</li>
 *   <li>Manual intervention to fix invalid data</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     Task task = jsonMapper.readValue(json, Task.class);
 * } catch (JsonProcessingException e) {
 *     throw new TaskSerializationException(taskId, "Failed to deserialize task", e);
 * }
 * }</pre>
 *
 * @see TaskStoreException
 * @see TaskPersistenceException for database failures
 */
public class TaskSerializationException extends TaskStoreException {

    /**
     * Creates a new TaskSerializationException with no message or cause.
     */
    public TaskSerializationException() {
        super();
    }

    /**
     * Creates a new TaskSerializationException with the specified message.
     *
     * @param msg the exception message
     */
    public TaskSerializationException(final String msg) {
        super(msg);
    }

    /**
     * Creates a new TaskSerializationException with the specified cause.
     *
     * @param cause the underlying cause
     */
    public TaskSerializationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new TaskSerializationException with the specified message and cause.
     *
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskSerializationException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * Creates a new TaskSerializationException with the specified task ID and message.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     */
    public TaskSerializationException(@Nullable final String taskId, final String msg) {
        super(taskId, msg);
    }

    /**
     * Creates a new TaskSerializationException with the specified task ID, message, and cause.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskSerializationException(@Nullable final String taskId, final String msg, final Throwable cause) {
        super(taskId, msg, cause);
    }
}
