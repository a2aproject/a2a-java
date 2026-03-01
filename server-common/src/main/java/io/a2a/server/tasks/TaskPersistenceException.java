package io.a2a.server.tasks;

import org.jspecify.annotations.Nullable;

/**
 * Exception for database/storage system failures during task persistence operations.
 * <p>
 * Indicates failures in the underlying storage system (database, filesystem, etc.) rather
 * than data format issues.
 *
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li>Database connection timeout or network partition</li>
 *   <li>Transaction deadlock or lock wait timeout</li>
 *   <li>Connection pool exhausted</li>
 *   <li>Disk full / storage quota exceeded</li>
 *   <li>Database constraint violations (unique key, foreign key)</li>
 *   <li>Insufficient permissions or authentication failures</li>
 *   <li>Database schema incompatibilities</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     em.merge(jpaTask);
 * } catch (PersistenceException e) {
 *     throw new TaskPersistenceException(taskId, "Database save failed", e);
 * }
 * }</pre>
 *
 * @see TaskStoreException
 * @see TaskSerializationException for data format errors
 */
public class TaskPersistenceException extends TaskStoreException {

    /**
     * Creates a new TaskPersistenceException with no message or cause.
     */
    public TaskPersistenceException() {
        super();
    }

    /**
     * Creates a new TaskPersistenceException with the specified message.
     *
     * @param msg the exception message
     */
    public TaskPersistenceException(final String msg) {
        super(msg);
    }

    /**
     * Creates a new TaskPersistenceException with the specified cause.
     *
     * @param cause the underlying cause
     */
    public TaskPersistenceException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new TaskPersistenceException with the specified message and cause.
     *
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskPersistenceException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * Creates a new TaskPersistenceException with the specified task ID and message.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     */
    public TaskPersistenceException(@Nullable final String taskId, final String msg) {
        super(taskId, msg);
    }

    /**
     * Creates a new TaskPersistenceException with the specified task ID, message, and cause.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskPersistenceException(@Nullable final String taskId, final String msg, final Throwable cause) {
        super(taskId, msg, cause);
    }
}
