package org.a2aproject.sdk.server.tasks;

import static org.a2aproject.sdk.spec.TaskState.TASK_STATE_FAILED;
import static org.a2aproject.sdk.spec.TaskState.TASK_STATE_SUBMITTED;
import static org.a2aproject.sdk.spec.util.Utils.appendArtifactToTask;
import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AServerException;
import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);

    private volatile @Nullable String taskId;
    private volatile @Nullable String contextId;
    private final TaskStore taskStore;
    private final @Nullable Message initialMessage;
    private volatile @Nullable Task currentTask;

    public TaskManager(@Nullable String taskId, @Nullable String contextId, TaskStore taskStore, @Nullable Message initialMessage) {
        checkNotNullParam("taskStore", taskStore);
        this.taskId = taskId;
        this.contextId = contextId;
        this.taskStore = taskStore;
        this.initialMessage = initialMessage;
    }

    @Nullable String getTaskId() {
        return taskId;
    }

    @Nullable String getContextId() {
        return contextId;
    }

    public @Nullable Task getTask() {
        if (taskId == null) {
            return null;
        }
        if (currentTask != null) {
            return currentTask;
        }
        currentTask = taskStore.get(taskId);
        return currentTask;
    }

    boolean saveTaskEvent(Task task, boolean isReplicated) throws A2AServerException {
        return saveTaskEvent(task, isReplicated, null);
    }

    boolean saveTaskEvent(Task task, boolean isReplicated, @Nullable AtomicReference<Task> taskSnapshot)
            throws A2AServerException {
        checkIdsAndUpdateIfNecessary(task.id(), task.contextId());
        // Defensive state-machine check: a task that already reached a terminal state must
        // not be overwritten by a task snapshot carrying a different state.
        Task current = getTask();
        if (current != null && current.status() != null && current.status().state() != null
                && task.status() != null && task.status().state() != null) {
            validateStateTransition(current.status().state(), task.status().state(), task.id());
        }
        Task savedTask = saveTask(task, isReplicated);
        if (taskSnapshot != null) {
            taskSnapshot.set(savedTask);
        }
        return savedTask.status() != null && savedTask.status().state() != null && savedTask.status().state().isFinal();
    }

    boolean saveTaskEvent(TaskStatusUpdateEvent event, boolean isReplicated) throws A2AServerException {
        return saveTaskEvent(event, isReplicated, null);
    }

    boolean saveTaskEvent(TaskStatusUpdateEvent event, boolean isReplicated, @Nullable AtomicReference<Task> taskSnapshot)
            throws A2AServerException {
        checkIdsAndUpdateIfNecessary(event.taskId(), event.contextId());
        Task task = ensureTask(event.taskId(), event.contextId());

        // State-machine validation: reject transitions that would overwrite a terminal
        // state with a different state. Re-arriving events carrying the same
        // final state remain allowed (idempotent replays / replication).
        TaskState currentState = task.status() != null ? task.status().state() : null;
        TaskState newState = event.status() != null ? event.status().state() : null;
        validateStateTransition(currentState, newState, event.taskId());

        Task.Builder builder = Task.builder(task)
                .status(event.status());

        if (task.status().message() != null) {
            List<Message> newHistory = task.history() == null ? new ArrayList<>() : new ArrayList<>(task.history());
            newHistory.add(task.status().message());
            builder.history(newHistory);
        }

        // Handle metadata from the event
        if (event.metadata() != null) {
            Map<String, Object> metadata = task.metadata() == null ? new HashMap<>() : new HashMap<>(task.metadata());
            metadata.putAll(event.metadata());
            builder.metadata(metadata);
        }

        task = builder.build();
        Task savedTask = saveTask(task, isReplicated);
        if (taskSnapshot != null) {
            taskSnapshot.set(savedTask);
        }
        return savedTask.status() != null && savedTask.status().state() != null && savedTask.status().state().isFinal();
    }

    boolean saveTaskEvent(TaskArtifactUpdateEvent event, boolean isReplicated) throws A2AServerException {
        return saveTaskEvent(event, isReplicated, null);
    }

    boolean saveTaskEvent(TaskArtifactUpdateEvent event, boolean isReplicated, @Nullable AtomicReference<Task> taskSnapshot)
            throws A2AServerException {
        checkIdsAndUpdateIfNecessary(event.taskId(), event.contextId());
        Task task = ensureTask(event.taskId(), event.contextId());
        // taskId is guaranteed to be non-null after checkIdsAndUpdateIfNecessary
        String nonNullTaskId = taskId;
        if (nonNullTaskId == null) {
            throw new IllegalStateException("taskId should not be null after checkIdsAndUpdateIfNecessary");
        }
        task = appendArtifactToTask(task, event, nonNullTaskId);
        Task savedTask = saveTask(task, isReplicated);
        if (taskSnapshot != null) {
            taskSnapshot.set(savedTask);
        }
        return savedTask.status() != null && savedTask.status().state() != null && savedTask.status().state().isFinal();
    }

    public boolean process(Event event, boolean isReplicated) throws A2AServerException {
        return process(event, isReplicated, null);
    }

    public boolean process(Event event, boolean isReplicated, @Nullable AtomicReference<Task> taskSnapshot)
            throws A2AServerException {
        boolean isFinal = false;
        if (event instanceof Task task) {
            isFinal = saveTaskEvent(task, isReplicated, taskSnapshot);
        } else if (event instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
            isFinal = saveTaskEvent(taskStatusUpdateEvent, isReplicated, taskSnapshot);
        } else if (event instanceof TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
            isFinal = saveTaskEvent(taskArtifactUpdateEvent, isReplicated, taskSnapshot);
        } else if (event instanceof A2AError) {
            // A2AError events trigger automatic transition to FAILED state
            // Error details are NOT persisted in TaskStore (client-specific)
            // Only the FAILED status is persisted and replicated across nodes

            // A2AError events don't have taskId/contextId fields, so we need to ensure
            // we have these from the existing task or TaskManager state
            if (taskId == null) {
                // No task context - A2AError event will be distributed to clients but no state update
                LOGGER.debug("A2AError event without task context - skipping state update");
                return true;  // Return true (is final) to stop event consumption
            }

            // Ensure we have contextId - get from existing task if not set
            String errorContextId = contextId;
            if (errorContextId == null) {
                Task existingTask = getTask();
                if (existingTask != null) {
                    errorContextId = existingTask.contextId();
                }
            }

            // Only create status update if we have contextId
            if (errorContextId != null) {
                LOGGER.debug("A2AError event detected, transitioning task {} to FAILED", taskId);
                TaskStatusUpdateEvent failedEvent = TaskStatusUpdateEvent.builder()
                        .taskId(taskId)
                        .contextId(errorContextId)
                        .status(new TaskStatus(TASK_STATE_FAILED))
                        .build();
                try {
                    isFinal = saveTaskEvent(failedEvent, isReplicated, taskSnapshot);
                } catch (A2AServerException e) {
                    // Task already in a terminal state: the state-machine guard in
                    // validateStateTransition rejected the synthesized FAILED event
                    // (terminal -> FAILED). No state update is needed — the A2AError
                    // itself still signals finality to clients.
                    LOGGER.debug("A2AError for task {} already in terminal state - skipping state update", taskId);
                    isFinal = true;
                }
            } else {
                // Can't update status without contextId, but error is still terminal
                LOGGER.debug("A2AError event for task {} without contextId - skipping state update", taskId);
                isFinal = true;
            }
        }
        return isFinal;
    }

    public Task updateWithMessage(Message message, Task task) {
        List<Message> history = new ArrayList<>(task.history());

        TaskStatus status = task.status();
        if (status.message() != null) {
            history.add(status.message());
            status = new TaskStatus(status.state(), null, status.timestamp());
        }
        history.add(message);
        task = Task.builder(task)
                .status(status)
                .history(history)
                .build();
        saveTask(task, false);  // Local operation, not replicated
        return task;
    }

    private void checkIdsAndUpdateIfNecessary(String eventTaskId, String eventContextId) throws A2AServerException {
        if (taskId != null && !eventTaskId.equals(taskId)) {
            throw new A2AServerException(
                    "Invalid task id",
                    new InternalError(String.format("Task event has taskId %s but TaskManager has %s", eventTaskId, taskId)));
        }
        if (taskId == null) {
            taskId = eventTaskId;
        }
        if (contextId == null) {
            contextId = eventContextId;
        }
    }

    private Task ensureTask(String eventTaskId, String eventContextId) {
        Task task = currentTask;
        if (task != null) {
            return task;
        }
        // taskId may be null here, but get() accepts @Nullable
        String currentTaskId = taskId;
        if (currentTaskId != null) {
            task = taskStore.get(currentTaskId);
        }
        if (task == null) {
            task = createTask(eventTaskId, eventContextId);
            saveTask(task, false);  // Local operation, not replicated
        }
        return task;
    }

    /**
     * Validates a task state transition before it is persisted.
     * <p>
     * A terminal (final) state must not be overwritten by a <em>different</em> state:
     * once a task is {@code COMPLETED}/{@code FAILED}/{@code CANCELED}/{@code REJECTED}
     * it stays in that state. Events re-arriving with the <em>same</em> final state are
     * allowed, so replicated replays and idempotent retries keep working.
     * <p>
     * Transitions from any non-terminal state to any state are permitted (e.g.
     * SUBMITTED → WORKING → COMPLETED/FAILED/CANCELED, interrupted-state resume flows),
     * matching the transitions the A2A spec and the reference agents exercise.
     *
     * @param currentState the task's current state, or {@code null} if unknown
     * @param newState     the state requested by the event, or {@code null} if unknown
     * @param taskId       the task identifier, used in the error message
     * @throws A2AServerException if the transition would overwrite a terminal state
     */
    private static void validateStateTransition(@Nullable TaskState currentState, @Nullable TaskState newState,
            String taskId) throws A2AServerException {
        if (currentState == null || newState == null) {
            return;
        }
        if (currentState.isFinal() && currentState != newState) {
            throw new A2AServerException(
                    "Task " + taskId + " is already in terminal state " + currentState
                            + " and cannot transition to " + newState,
                    new InternalError("Task " + taskId + " is already in terminal state " + currentState));
        }
    }

    private Task createTask(String taskId, String contextId) {
        List<Message> history = initialMessage != null ? List.of(initialMessage) : Collections.emptyList();
        return Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TASK_STATE_SUBMITTED))
                .history(history)
                .build();
    }

    private Task saveTask(Task task, boolean isReplicated) {
        taskStore.save(task, isReplicated);
        if (taskId == null) {
            taskId = task.id();
            contextId = task.contextId();
        }
        currentTask = task;
        return currentTask;
    }
}
