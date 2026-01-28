package io.a2a.server.tasks;

import static io.a2a.spec.TaskState.SUBMITTED;
import static io.a2a.util.Assert.checkNotNullParam;
import static io.a2a.util.Utils.appendArtifactToTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private volatile boolean isTempId;

    public TaskManager(@Nullable String taskId, @Nullable String contextId, TaskStore taskStore, @Nullable Message initialMessage, boolean isTempId) {
        checkNotNullParam("taskStore", taskStore);
        this.taskId = taskId;
        this.contextId = contextId;
        this.taskStore = taskStore;
        this.initialMessage = initialMessage;
        this.isTempId = isTempId;
    }

    /**
     * Updates the taskId from a temporary ID to the real task ID.
     * Only allowed when this TaskManager was created with isTempId=true.
     * Called by DefaultRequestHandler when switching from temp-UUID to real task.id.
     */
    public void setTaskId(String newTaskId) {
        if (!isTempId) {
            throw new IllegalStateException("Cannot change taskId - not created with temporary ID");
        }
        LOGGER.debug("TaskManager switching taskId from {} to {}", this.taskId, newTaskId);
        this.taskId = newTaskId;
        this.isTempId = false; // No longer temporary after switch
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

    Task saveTaskEvent(Task task) throws A2AServerException {
        checkIdsAndUpdateIfNecessary(task.id(), task.contextId());
        return saveTask(task);
    }

    Task saveTaskEvent(TaskStatusUpdateEvent event) throws A2AServerException {
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
        return saveTask(task);
    }

    Task saveTaskEvent(TaskArtifactUpdateEvent event) throws A2AServerException {
        checkIdsAndUpdateIfNecessary(event.taskId(), event.contextId());
        Task task = ensureTask(event.taskId(), event.contextId());
        // taskId is guaranteed to be non-null after checkIdsAndUpdateIfNecessary
        String nonNullTaskId = taskId;
        if (nonNullTaskId == null) {
            throw new IllegalStateException("taskId should not be null after checkIdsAndUpdateIfNecessary");
        }
        task = appendArtifactToTask(task, event, nonNullTaskId);
        return saveTask(task);
    }

    public Event process(Event event) throws A2AServerException {
        if (event instanceof Task task) {
            saveTaskEvent(task);
        } else if (event instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
            saveTaskEvent(taskStatusUpdateEvent);
        } else if (event instanceof TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
            saveTaskEvent(taskArtifactUpdateEvent);
        }
        return event;
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
        saveTask(task);
        return task;
    }

    private void checkIdsAndUpdateIfNecessary(String eventTaskId, String eventContextId) throws A2AServerException {
        if (taskId != null && !eventTaskId.equals(taskId)) {
            // Allow switching from temporary ID to real task ID
            // This happens when client sends message without taskId and agent creates Task with real ID
            if (isTempId) {
                // Verify the new task ID doesn't already exist in the store
                // If it does, the agent is trying to return an existing task when it should create a new one
                Task existingTask = taskStore.get(eventTaskId);
                if (existingTask != null) {
                    throw new A2AServerException(
                            "Invalid task id",
                            new InternalError(String.format("Agent returned existing task ID %s when expecting new task", eventTaskId)));
                }
                LOGGER.debug("TaskManager allowing taskId switch from temp {} to real {}", taskId, eventTaskId);
                taskId = eventTaskId;
                isTempId = false; // No longer temporary after switch
            } else {
                throw new A2AServerException(
                        "Invalid task id",
                        new InternalError(String.format("Task in event doesn't match TaskManager ")));
            }
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
            saveTask(task);
        }
        return task;
    }

    private Task createTask(String taskId, String contextId) {
        List<Message> history = initialMessage != null ? List.of(initialMessage) : Collections.emptyList();
        return Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(SUBMITTED))
                .history(history)
                .build();
    }

    private Task saveTask(Task task) {
        taskStore.save(task);
        if (taskId == null) {
            taskId = task.id();
            contextId = task.contextId();
        }
        currentTask = task;
        return currentTask;
    }
}
