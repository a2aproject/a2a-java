package io.a2a.server.tasks;

import org.jspecify.annotations.Nullable;

import io.a2a.spec.ListTasksParams;
import io.a2a.spec.ListTasksResult;
import io.a2a.spec.Task;

/**
 * Storage interface for managing task persistence.
 * <p>
 * Implementations can use in-memory storage, databases, or other persistence mechanisms.
 * Default implementation is {@code InMemoryTaskStore}.
 * </p>
 */
public interface TaskStore {
    /**
     * Saves or updates a task.
     *
     * @param task the task to save
     */
    void save(Task task);

    /**
     * Retrieves a task by its ID.
     *
     * @param taskId the task identifier
     * @return the task if found, null otherwise
     */
    @Nullable Task get(String taskId);

    /**
     * Deletes a task by its ID.
     *
     * @param taskId the task identifier
     */
    void delete(String taskId);

    /**
     * List tasks with optional filtering and pagination.
     *
     * @param params the filtering and pagination parameters
     * @return the list of tasks matching the criteria with pagination info
     */
    ListTasksResult list(ListTasksParams params);
}
