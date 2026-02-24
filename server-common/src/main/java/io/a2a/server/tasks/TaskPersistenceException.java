package io.a2a.server.tasks;

import org.jspecify.annotations.Nullable;

/**
 * Exception for database/storage system failures during task persistence operations.
 * <p>
 * Indicates failures in the underlying storage system (database, filesystem, etc.) rather
 * than data format issues. Includes a {@link #isTransient()} flag to distinguish between
 * temporary failures (retry recommended) and permanent failures (manual intervention required).
 *
 * <h2>Transient Failures (isTransient = true)</h2>
 * Temporary issues that may resolve with retry:
 * <ul>
 *   <li>Database connection timeout or network partition</li>
 *   <li>Transaction deadlock or lock wait timeout</li>
 *   <li>Connection pool exhausted</li>
 *   <li>Temporary storage system overload</li>
 * </ul>
 * <b>Recovery</b>: Exponential backoff retry, circuit breaker patterns
 *
 * <h2>Non-Transient Failures (isTransient = false)</h2>
 * Persistent issues requiring intervention:
 * <ul>
 *   <li>Disk full / storage quota exceeded</li>
 *   <li>Database constraint violations (unique key, foreign key)</li>
 *   <li>Insufficient permissions or authentication failures</li>
 *   <li>Database schema incompatibilities</li>
 * </ul>
 * <b>Recovery</b>: Manual intervention, capacity planning, configuration fixes
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     em.merge(jpaTask);
 * } catch (PersistenceException e) {
 *     boolean transient = isTransientDatabaseError(e);
 *     throw new TaskPersistenceException(taskId, "Database save failed", e, transient);
 * }
 * }</pre>
 *
 * @see TaskStoreException
 * @see TaskSerializationException for data format errors
 */
public class TaskPersistenceException extends TaskStoreException {

    private final boolean isTransientFailure;

    /**
     * Creates a new TaskPersistenceException with no message or cause.
     * The failure is assumed to be non-transient.
     */
    public TaskPersistenceException() {
        super();
        this.isTransientFailure = false;
    }

    /**
     * Creates a new TaskPersistenceException with the specified message.
     * The failure is assumed to be non-transient.
     *
     * @param msg the exception message
     */
    public TaskPersistenceException(final String msg) {
        super(msg);
        this.isTransientFailure = false;
    }

    /**
     * Creates a new TaskPersistenceException with the specified cause.
     * The failure is assumed to be non-transient.
     *
     * @param cause the underlying cause
     */
    public TaskPersistenceException(final Throwable cause) {
        super(cause);
        this.isTransientFailure = false;
    }

    /**
     * Creates a new TaskPersistenceException with the specified message and cause.
     * The failure is assumed to be non-transient.
     *
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskPersistenceException(final String msg, final Throwable cause) {
        super(msg, cause);
        this.isTransientFailure = false;
    }

    /**
     * Creates a new TaskPersistenceException with the specified task ID and message.
     * The failure is assumed to be non-transient.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     */
    public TaskPersistenceException(@Nullable final String taskId, final String msg) {
        super(taskId, msg);
        this.isTransientFailure = false;
    }

    /**
     * Creates a new TaskPersistenceException with the specified task ID, message, and cause.
     * The failure is assumed to be non-transient.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskPersistenceException(@Nullable final String taskId, final String msg, final Throwable cause) {
        super(taskId, msg, cause);
        this.isTransientFailure = false;
    }

    /**
     * Creates a new TaskPersistenceException with the specified message and transient flag.
     *
     * @param msg the exception message
     * @param isTransient true if the failure is transient and may resolve with retry, false otherwise
     */
    public TaskPersistenceException(final String msg, final boolean isTransient) {
        super(msg);
        this.isTransientFailure = isTransient;
    }

    /**
     * Creates a new TaskPersistenceException with the specified message, cause, and transient flag.
     *
     * @param msg the exception message
     * @param cause the underlying cause
     * @param isTransient true if the failure is transient and may resolve with retry, false otherwise
     */
    public TaskPersistenceException(final String msg, final Throwable cause, final boolean isTransient) {
        super(msg, cause);
        this.isTransientFailure = isTransient;
    }

    /**
     * Creates a new TaskPersistenceException with the specified task ID, message, and transient flag.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     * @param isTransient true if the failure is transient and may resolve with retry, false otherwise
     */
    public TaskPersistenceException(@Nullable final String taskId, final String msg, final boolean isTransient) {
        super(taskId, msg);
        this.isTransientFailure = isTransient;
    }

    /**
     * Creates a new TaskPersistenceException with the specified task ID, message, cause, and transient flag.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     * @param cause the underlying cause
     * @param isTransient true if the failure is transient and may resolve with retry, false otherwise
     */
    public TaskPersistenceException(@Nullable final String taskId, final String msg, final Throwable cause,
                                     final boolean isTransient) {
        super(taskId, msg, cause);
        this.isTransientFailure = isTransient;
    }

    /**
     * Indicates whether this failure is transient (retry may help).
     *
     * @return true if transient (network, timeout, deadlock), false if permanent (disk full, constraint)
     */
    public boolean isTransient() {
        return isTransientFailure;
    }
}
