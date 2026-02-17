package io.a2a.server.tasks;

import static io.a2a.spec.TaskState.TASK_STATE_FAILED;
import static io.a2a.spec.TaskState.TASK_STATE_SUBMITTED;
import static io.a2a.util.Assert.checkNotNullParam;
import static io.a2a.util.Utils.appendArtifactToTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.a2a.spec.A2AError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.InternalError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
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
        checkIdsAndUpdateIfNecessary(task.id(), task.contextId());
        Task savedTask = saveTask(task, isReplicated);
        return savedTask.status() != null && savedTask.status().state() != null && savedTask.status().state().isFinal();
    }

    boolean saveTaskEvent(TaskStatusUpdateEvent event, boolean isReplicated) throws A2AServerException {
        checkIdsAndUpdateIfNecessary(event.taskId(), event.contextId());
        Task task = ensureTask(event.taskId(), event.contextId());


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
        return savedTask.status() != null && savedTask.status().state() != null && savedTask.status().state().isFinal();
    }

    boolean saveTaskEvent(TaskArtifactUpdateEvent event, boolean isReplicated) throws A2AServerException {
        checkIdsAndUpdateIfNecessary(event.taskId(), event.contextId());
        Task task = ensureTask(event.taskId(), event.contextId());
        // taskId is guaranteed to be non-null after checkIdsAndUpdateIfNecessary
        String nonNullTaskId = taskId;
        if (nonNullTaskId == null) {
            throw new IllegalStateException("taskId should not be null after checkIdsAndUpdateIfNecessary");
        }
        task = appendArtifactToTask(task, event, nonNullTaskId);
        Task savedTask = saveTask(task, isReplicated);
        return savedTask.status() != null && savedTask.status().state() != null && savedTask.status().state().isFinal();
    }

    public boolean process(Event event, boolean isReplicated) throws A2AServerException {
        boolean isFinal = false;
        if (event instanceof Task task) {
            isFinal = saveTaskEvent(task, isReplicated);
        } else if (event instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
            isFinal = saveTaskEvent(taskStatusUpdateEvent, isReplicated);
        } else if (event instanceof TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
            isFinal = saveTaskEvent(taskArtifactUpdateEvent, isReplicated);
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
                isFinal = saveTaskEvent(failedEvent, isReplicated);
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
