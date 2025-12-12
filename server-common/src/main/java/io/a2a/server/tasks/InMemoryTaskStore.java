package io.a2a.server.tasks;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

import org.jspecify.annotations.Nullable;

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
    public @Nullable Task get(String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public void delete(String taskId) {
        tasks.remove(taskId);
    }

    @Override
    public ListTasksResult list(ListTasksParams params) {
        // Filter and sort tasks in a single stream pipeline
        List<Task> allFilteredTasks = tasks.values().stream()
                .filter(task -> params.contextId() == null || params.contextId().equals(task.getContextId()))
                .filter(task -> params.status() == null ||
                        (task.getStatus() != null && params.status().equals(task.getStatus().state())))
                .filter(task -> params.lastUpdatedAfter() == null ||
                        (task.getStatus() != null &&
                         task.getStatus().timestamp() != null &&
                         task.getStatus().timestamp().toInstant().isAfter(params.lastUpdatedAfter())))
                .sorted(Comparator.comparing(
                        (Task t) -> (t.getStatus() != null && t.getStatus().timestamp() != null)
                                // Truncate to milliseconds for consistency with pageToken precision
                                ? t.getStatus().timestamp().toInstant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                                : null,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Task::getId))
                .toList();

        int totalSize = allFilteredTasks.size();

        // Apply pagination
        int pageSize = params.getEffectivePageSize();
        int startIndex = 0;

        // Handle page token using keyset pagination (format: "timestamp_millis:taskId")
        // Use binary search to efficiently find the first task after the pageToken position (O(log N))
        if (params.pageToken() != null && !params.pageToken().isEmpty()) {
            String[] tokenParts = params.pageToken().split(":", 2);
            if (tokenParts.length == 2) {
                try {
                    long tokenTimestampMillis = Long.parseLong(tokenParts[0]);
                    java.time.Instant tokenTimestamp = java.time.Instant.ofEpochMilli(tokenTimestampMillis);
                    String tokenId = tokenParts[1];

                    // Binary search for first task where: timestamp < tokenTimestamp OR (timestamp == tokenTimestamp AND id > tokenId)
                    // Since list is sorted (timestamp DESC, id ASC), we search for the insertion point
                    int left = 0;
                    int right = allFilteredTasks.size();

                    while (left < right) {
                        int mid = left + (right - left) / 2;
                        Task task = allFilteredTasks.get(mid);

                        // All tasks have timestamps (TaskStatus canonical constructor ensures this)
                        // Truncate to milliseconds for consistency with pageToken precision
                        java.time.Instant taskTimestamp = task.getStatus().timestamp().toInstant()
                                .truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
                        int timestampCompare = taskTimestamp.compareTo(tokenTimestamp);

                        if (timestampCompare < 0 || (timestampCompare == 0 && task.getId().compareTo(tokenId) > 0)) {
                            // This task is after the token, search left half
                            right = mid;
                        } else {
                            // This task is before or equal to token, search right half
                            left = mid + 1;
                        }
                    }
                    startIndex = left;
                } catch (NumberFormatException e) {
                    // Malformed timestamp in pageToken
                    throw new io.a2a.spec.InvalidParamsError(null,
                        "Invalid pageToken format: timestamp must be numeric milliseconds", null);
                }
            } else {
                // Legacy ID-only pageToken format is not supported with timestamp-based sorting
                // Throw error to prevent incorrect pagination results
                throw new io.a2a.spec.InvalidParamsError(null, "Invalid pageToken format: expected 'timestamp:id'", null);
            }
        }

        // Get the page of tasks
        int endIndex = Math.min(startIndex + pageSize, allFilteredTasks.size());
        List<Task> pageTasks = allFilteredTasks.subList(startIndex, endIndex);

        // Determine next page token (format: "timestamp_millis:taskId")
        String nextPageToken = null;
        if (endIndex < allFilteredTasks.size()) {
            Task lastTask = allFilteredTasks.get(endIndex - 1);
            // All tasks have timestamps (TaskStatus canonical constructor ensures this)
            long timestampMillis = lastTask.getStatus().timestamp().toInstant().toEpochMilli();
            nextPageToken = timestampMillis + ":" + lastTask.getId();
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
