package io.a2a.server.tasks;

import io.a2a.spec.A2AServerException;
import org.jspecify.annotations.Nullable;

/**
 * Base exception for TaskStore persistence layer failures.
 * <p>
 * Root exception for all task storage and retrieval errors. Specialized subclasses
 * provide specific failure contexts:
 * <ul>
 *   <li>{@link TaskSerializationException} - JSON/data format errors</li>
 *   <li>{@link TaskPersistenceException} - Database/storage system failures</li>
 * </ul>
 *
 * <h2>Usage Context</h2>
 * Thrown by {@link TaskStore} implementations during:
 * <ul>
 *   <li>{@code save(Task, boolean)} - Task persistence failures</li>
 *   <li>{@code get(String)} - Task retrieval failures</li>
 *   <li>{@code delete(String)} - Task deletion failures</li>
 *   <li>{@code list(ListTasksParams)} - Task listing failures</li>
 * </ul>
 *
 * <h2>Error Handling Pattern</h2>
 * Caught by {@link io.a2a.server.events.MainEventBusProcessor} which:
 * <ol>
 *   <li>Logs the failure with full context (taskId, operation)</li>
 *   <li>Distributes {@link io.a2a.spec.InternalError} event to clients</li>
 *   <li>Preserves exception cause chain for diagnostics</li>
 * </ol>
 *
 * @see TaskSerializationException for data format errors
 * @see TaskPersistenceException for storage system failures
 * @see TaskStore
 */
public class TaskStoreException extends A2AServerException {

    @Nullable
    private final String taskId;

    /**
     * Creates a new TaskStoreException with no message or cause.
     */
    public TaskStoreException() {
        super();
        this.taskId = null;
    }

    /**
     * Creates a new TaskStoreException with the specified message.
     *
     * @param msg the exception message
     */
    public TaskStoreException(final String msg) {
        super(msg);
        this.taskId = null;
    }

    /**
     * Creates a new TaskStoreException with the specified cause.
     *
     * @param cause the underlying cause
     */
    public TaskStoreException(final Throwable cause) {
        super(cause);
        this.taskId = null;
    }

    /**
     * Creates a new TaskStoreException with the specified message and cause.
     *
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskStoreException(final String msg, final Throwable cause) {
        super(msg, cause);
        this.taskId = null;
    }

    /**
     * Creates a new TaskStoreException with the specified task ID and message.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     */
    public TaskStoreException(@Nullable final String taskId, final String msg) {
        super(msg);
        this.taskId = taskId;
    }

    /**
     * Creates a new TaskStoreException with the specified task ID, message, and cause.
     *
     * @param taskId the task identifier (may be null for operations not tied to a specific task)
     * @param msg the exception message
     * @param cause the underlying cause
     */
    public TaskStoreException(@Nullable final String taskId, final String msg, final Throwable cause) {
        super(msg, cause);
        this.taskId = taskId;
    }

    /**
     * Returns the task ID associated with this exception.
     *
     * @return the task ID, or null if not associated with a specific task
     */
    @Nullable
    public String getTaskId() {
        return taskId;
    }
}
