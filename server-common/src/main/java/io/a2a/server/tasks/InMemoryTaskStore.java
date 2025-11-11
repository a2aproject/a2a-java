package io.a2a.server.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import io.a2a.spec.Artifact;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.ListTasksResult;
import io.a2a.spec.Message;
import io.a2a.spec.Task;

@ApplicationScoped
public class InMemoryTaskStore implements TaskStore, TaskStateProvider {

    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void save(Task task) {
        tasks.put(task.getId(), task);
    }

    @Override
    public Task get(String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public void delete(String taskId) {
        tasks.remove(taskId);
    }

    @Override
    public ListTasksResult list(ListTasksParams params) {
        Stream<Task> taskStream = tasks.values().stream();

        // Apply filters
        if (params.contextId() != null) {
            taskStream = taskStream.filter(task -> params.contextId().equals(task.getContextId()));
        }
        if (params.status() != null) {
            taskStream = taskStream.filter(task ->
                task.getStatus() != null && params.status().equals(task.getStatus().state())
            );
        }
        // Note: lastUpdatedAfter filtering not implemented in InMemoryTaskStore
        // as Task doesn't have a lastUpdated timestamp field

        // Sort by task ID for consistent pagination
        List<Task> allFilteredTasks = taskStream
                .sorted(Comparator.comparing(Task::getId))
                .toList();

        int totalSize = allFilteredTasks.size();

        // Apply pagination
        int pageSize = params.getEffectivePageSize();
        int startIndex = 0;

        // Handle page token (simple cursor: last task ID from previous page)
        if (params.pageToken() != null && !params.pageToken().isEmpty()) {
            // Use binary search since list is sorted by task ID (O(log N) vs O(N))
            int index = Collections.binarySearch(allFilteredTasks, null,
                (t1, t2) -> {
                    // Handle null key comparisons (binarySearch passes null as one argument)
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return params.pageToken().compareTo(t2.getId());
                    if (t2 == null) return t1.getId().compareTo(params.pageToken());
                    return t1.getId().compareTo(t2.getId());
                });
            if (index >= 0) {
                startIndex = index + 1;
            }
            // If not found (index < 0), startIndex remains 0 (start from beginning)
        }

        // Get the page of tasks
        int endIndex = Math.min(startIndex + pageSize, allFilteredTasks.size());
        List<Task> pageTasks = allFilteredTasks.subList(startIndex, endIndex);

        // Determine next page token
        String nextPageToken = null;
        if (endIndex < allFilteredTasks.size()) {
            nextPageToken = allFilteredTasks.get(endIndex - 1).getId();
        }

        // Transform tasks: limit history and optionally remove artifacts
        int historyLength = params.getEffectiveHistoryLength();
        boolean includeArtifacts = params.shouldIncludeArtifacts();

        List<Task> transformedTasks = pageTasks.stream()
                .map(task -> transformTask(task, historyLength, includeArtifacts))
                .toList();

        return new ListTasksResult(transformedTasks, totalSize, transformedTasks.size(), nextPageToken);
    }

    private Task transformTask(Task task, int historyLength, boolean includeArtifacts) {
        // Limit history if needed (keep most recent N messages)
        List<Message> history = task.getHistory();
        if (historyLength > 0 && history != null && history.size() > historyLength) {
            history = history.subList(history.size() - historyLength, history.size());
        }

        // Remove artifacts if not requested
        List<Artifact> artifacts = includeArtifacts ? task.getArtifacts() : List.of();

        // If no transformation needed, return original task
        if (history == task.getHistory() && artifacts == task.getArtifacts()) {
            return task;
        }

        // Build new task with transformed data
        return new Task.Builder(task)
                .artifacts(artifacts)
                .history(history)
                .build();
    }

    @Override
    public boolean isTaskActive(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        // Task is active if not in final state
        return task.getStatus() == null || task.getStatus().state() == null || !task.getStatus().state().isFinal();
    }

    @Override
    public boolean isTaskFinalized(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        // Task is finalized if in final state (ignores grace period)
        return task.getStatus() != null
                && task.getStatus().state() != null
                && task.getStatus().state().isFinal();
    }
}
