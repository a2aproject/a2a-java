package io.a2a.server.tasks;

import java.util.HashSet;
import java.util.Set;

/**
 * Mock implementation of TaskStateProvider for testing.
 * Allows tests to explicitly control which tasks are considered finalized.
 */
public class MockTaskStateProvider implements TaskStateProvider {
    private final Set<String> finalizedTasks = new HashSet<>();

    /**
     * Mark a task as finalized for testing purposes.
     *
     * @param taskId the task ID to mark as finalized
     */
    public void markFinalized(String taskId) {
        finalizedTasks.add(taskId);
    }

    /**
     * Unmark a task as finalized (return to active state).
     *
     * @param taskId the task ID to unmark
     */
    public void unmarkFinalized(String taskId) {
        finalizedTasks.remove(taskId);
    }

    @Override
    public boolean isTaskActive(String taskId) {
        return !finalizedTasks.contains(taskId);
    }

    @Override
    public boolean isTaskFinalized(String taskId) {
        return finalizedTasks.contains(taskId);
    }
}
