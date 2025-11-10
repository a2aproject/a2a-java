package io.a2a.server.tasks;


import io.a2a.spec.ListTasksParams;
import io.a2a.spec.ListTasksResult;
import io.a2a.spec.Task;

public interface TaskStore {
    void save(Task task);

    Task get(String taskId);

    void delete(String taskId);

    /**
     * List tasks with optional filtering and pagination.
     *
     * @param params the filtering and pagination parameters
     * @return the list of tasks matching the criteria with pagination info
     */
    ListTasksResult list(ListTasksParams params);
}
