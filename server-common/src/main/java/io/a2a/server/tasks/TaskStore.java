package io.a2a.server.tasks;

import io.a2a.spec.Task;

public interface TaskStore {
    void save(Task task);

    Task get(String taskId);

    void delete(String taskId);
}
