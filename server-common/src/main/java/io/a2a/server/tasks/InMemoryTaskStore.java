package io.a2a.server.tasks;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.spec.Artifact;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.util.PageToken;
import io.a2a.spec.Task;
import org.jspecify.annotations.Nullable;

/**
 * In-memory implementation of {@link TaskStore} and {@link TaskStateProvider}.
 * <p>
 * This implementation uses a {@link ConcurrentHashMap} to store tasks in memory.
 * Tasks are lost on application restart. For persistent storage, use a database-backed
 * implementation such as the JPA TaskStore in the extras module.
 * </p>
 * <p>
 * This is the default TaskStore used when no other implementation is provided.
 * </p>
 *
 * <h2>Exception Behavior</h2>
 * InMemoryTaskStore has minimal exception scenarios compared to database-backed implementations:
 * <ul>
 *   <li><b>No TaskSerializationException:</b> Task objects are stored directly in memory without
 *       serialization. No JSON parsing or schema compatibility issues can occur.</li>
 *   <li><b>No TaskPersistenceException:</b> ConcurrentHashMap operations do not involve I/O,
 *       network, or transactional concerns. Standard put/get/remove operations are guaranteed
 *       to succeed under normal JVM operation.</li>
 *   <li><b>OutOfMemoryError (potential):</b> The only failure scenario is JVM heap exhaustion if
 *       too many tasks are stored. This is an {@link Error} (not Exception) and indicates a fatal
 *       system condition requiring JVM restart and capacity planning.</li>
 * </ul>
 *
 * <h3>Design Rationale</h3>
 * This implementation intentionally does NOT throw {@link TaskStoreException} or its subclasses
 * because:
 * <ul>
 *   <li>No serialization step exists - tasks stored as Java objects</li>
 *   <li>No I/O or network operations that can fail</li>
 *   <li>ConcurrentHashMap guarantees thread-safe operations without checked exceptions</li>
 *   <li>Memory exhaustion (OutOfMemoryError) is an unrecoverable system failure</li>
 * </ul>
 *
 * <h3>Comparison to Database Implementations</h3>
 * Database-backed implementations (e.g., JpaDatabaseTaskStore) throw exceptions for:
 * <ul>
 *   <li>Serialization errors (JSON parsing, schema mismatches)</li>
 *   <li>Connection failures (network, timeouts)</li>
 *   <li>Transaction failures (deadlocks, constraint violations)</li>
 *   <li>Capacity issues (disk full, quota exceeded)</li>
 * </ul>
 * InMemoryTaskStore avoids all of these by operating entirely in-process.
 *
 * <h3>Memory Management Considerations</h3>
 * Callers should monitor memory usage and implement task cleanup policies:
 * <pre>{@code
 * // Example: Delete finalized tasks older than 48 hours
 * ListTasksParams params = new ListTasksParams.Builder()
 *     .statusTimestampBefore(Instant.now().minus(Duration.ofHours(48)))
 *     .build();
 *
 * List<Task> oldTasks = taskStore.list(params).tasks();
 * oldTasks.stream()
 *     .filter(task -> task.status().state().isFinal())
 *     .forEach(task -> taskStore.delete(task.id()));
 * }</pre>
 *
 * <h3>Thread Safety</h3>
 * All operations are thread-safe via {@link ConcurrentHashMap}. Multiple threads can
 * concurrently save, get, list, and delete tasks without synchronization. Last-write-wins
 * semantics apply for concurrent {@code save()} calls to the same task ID.
 *
 * @see TaskStore for interface contract and exception documentation
 * @see TaskStoreException for exception hierarchy (not thrown by this implementation)
 * @see TaskStateProvider for queue lifecycle integration
 */
@ApplicationScoped
public class InMemoryTaskStore implements TaskStore, TaskStateProvider {

    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void save(Task task, boolean isReplicated) {
        tasks.put(task.id(), task);
        // InMemoryTaskStore doesn't fire TaskFinalizedEvent, so isReplicated is unused here
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
                .filter(task -> params.contextId() == null || params.contextId().equals(task.contextId()))
                .filter(task -> params.status() == null ||
                        (task.status() != null && params.status().equals(task.status().state())))
                .filter(task -> params.statusTimestampAfter() == null ||
                        (task.status() != null &&
                         task.status().timestamp() != null &&
                         task.status().timestamp().toInstant().isAfter(params.statusTimestampAfter())))
                .sorted(Comparator.comparing(
                        (Task t) -> (t.status() != null && t.status().timestamp() != null)
                                // Truncate to milliseconds for consistency with pageToken precision
                                ? t.status().timestamp().toInstant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                                : null,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Task::id))
                .toList();

        int totalSize = allFilteredTasks.size();

        // Apply pagination
        int pageSize = params.getEffectivePageSize();
        int startIndex = 0;

        // Handle page token using keyset pagination (format: "timestamp_millis:taskId")
        // Use binary search to efficiently find the first task after the pageToken position (O(log N))
        PageToken pageToken = PageToken.fromString(params.pageToken());
        if (pageToken != null) {
            java.time.Instant tokenTimestamp = pageToken.timestamp();
            String tokenId = pageToken.id();

            // Binary search for first task where: timestamp < tokenTimestamp OR (timestamp == tokenTimestamp AND id > tokenId)
            // Since list is sorted (timestamp DESC, id ASC), we search for the insertion point
            int left = 0;
            int right = allFilteredTasks.size();

            while (left < right) {
                int mid = left + (right - left) / 2;
                Task task = allFilteredTasks.get(mid);

                java.time.Instant taskTimestamp = (task.status() != null && task.status().timestamp() != null)
                        ? task.status().timestamp().toInstant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                        : null;

                if (taskTimestamp == null) {
                    // Task with null timestamp is always "before" a token with a timestamp, as they are sorted last.
                    // So, we search in the right half.
                    left = mid + 1;
                } else {
                    int timestampCompare = taskTimestamp.compareTo(tokenTimestamp);

                    if (timestampCompare < 0 || (timestampCompare == 0 && task.id().compareTo(tokenId) > 0)) {
                        // This task is after the token, search left half
                        right = mid;
                    } else {
                        // This task is before or equal to token, search right half
                        left = mid + 1;
                    }
                }
            }
            startIndex = left;
        }

        // Get the page of tasks
        int endIndex = Math.min(startIndex + pageSize, allFilteredTasks.size());
        List<Task> pageTasks = allFilteredTasks.subList(startIndex, endIndex);

        // Determine next page token (format: "timestamp_millis:taskId")
        String nextPageToken = null;
        if (endIndex < allFilteredTasks.size()) {
            Task lastTask = allFilteredTasks.get(endIndex - 1);
            // All tasks have timestamps (TaskStatus canonical constructor ensures this)
            java.time.Instant timestamp = lastTask.status().timestamp().toInstant();
            nextPageToken = new PageToken(timestamp, lastTask.id()).toString();
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
        List<Message> history = task.history();
        if (historyLength == 0) {
            // When historyLength is 0, return empty history
            history = List.of();
        } else if (historyLength > 0 && history != null && history.size() > historyLength) {
            history = history.subList(history.size() - historyLength, history.size());
        }

        // Remove artifacts if not requested
        List<Artifact> artifacts = includeArtifacts ? task.artifacts() : List.of();

        // If no transformation needed, return original task
        if (history == task.history() && artifacts == task.artifacts()) {
            return task;
        }

        // Build new task with transformed data
        return Task.builder(task)
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
        return task.status() == null || task.status().state() == null || !task.status().state().isFinal();
    }

    @Override
    public boolean isTaskFinalized(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        // Task is finalized if in final state (ignores grace period)
        return task.status() != null
                && task.status().state() != null
                && task.status().state().isFinal();
    }
}
