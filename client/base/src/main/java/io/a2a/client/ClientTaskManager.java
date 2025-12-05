package io.a2a.client;

import static io.a2a.util.Utils.appendArtifactToTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientInvalidArgsError;
import io.a2a.spec.A2AClientInvalidStateError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.jspecify.annotations.Nullable;

/**
 * Helps manage a task's lifecycle during the execution of a request.
 * Responsible for retrieving, saving, and updating the task based on
 * events received from the agent.
 */
public class ClientTaskManager {

    private @Nullable Task currentTask;
    private @Nullable String taskId;
    private @Nullable String contextId;

    public ClientTaskManager() {
        this.currentTask = null;
        this.taskId = null;
        this.contextId = null;
    }

    public Task getCurrentTask() throws A2AClientInvalidStateError {
        if (currentTask == null) {
            throw new A2AClientInvalidStateError("No current task");
        }
        return currentTask;
    }

    public Task saveTaskEvent(Task task) throws A2AClientInvalidArgsError {
        if (currentTask != null) {
            throw new A2AClientInvalidArgsError("Task is already set, create new manager for new tasks.");
        }
        saveTask(task);
        return task;
    }

    public Task saveTaskEvent(TaskStatusUpdateEvent taskStatusUpdateEvent) throws A2AClientError {
        if (taskId == null) {
            taskId = taskStatusUpdateEvent.getTaskId();
        }
        if (contextId == null) {
            contextId = taskStatusUpdateEvent.getContextId();
        }
        Task task = currentTask;
        if (task == null) {
            task = new Task.Builder()
                    .status(new TaskStatus(TaskState.UNKNOWN))
                    .id(taskId)
                    .contextId(contextId == null ? "" : contextId)
                    .build();
        }

        Task.Builder taskBuilder = new Task.Builder(task);
        if (taskStatusUpdateEvent.getStatus().message() != null) {
            if (task.getHistory() == null) {
                taskBuilder.history(taskStatusUpdateEvent.getStatus().message());
            } else {
                List<Message> history = new ArrayList<>(task.getHistory());
                history.add(taskStatusUpdateEvent.getStatus().message());
                taskBuilder.history(history);
            }
        }
        if (taskStatusUpdateEvent.getMetadata() != null) {
            Map<String, Object> newMetadata = task.getMetadata() != null ? new HashMap<>(task.getMetadata()) : new HashMap<>();
            newMetadata.putAll(taskStatusUpdateEvent.getMetadata());
            taskBuilder.metadata(newMetadata);
        }
        taskBuilder.status(taskStatusUpdateEvent.getStatus());
        currentTask = taskBuilder.build();
        return currentTask;
    }

    public Task saveTaskEvent(TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
        if (taskId == null) {
            taskId = taskArtifactUpdateEvent.getTaskId();
        }
        if (contextId == null) {
            contextId = taskArtifactUpdateEvent.getContextId();
        }
        Task task = currentTask;
        if (task == null) {
            task = new Task.Builder()
                    .status(new TaskStatus(TaskState.UNKNOWN))
                    .id(taskId)
                    .contextId(contextId == null ? "" : contextId)
                    .build();
        }
        currentTask = appendArtifactToTask(task, taskArtifactUpdateEvent);
        return currentTask;
    }

    /**
     * Update a task by adding a message to its history. If the task has a message in its current status,
     * that message is moved to the history first.
     *
     * @param message the new message to add to the history
     * @param task the task to update
     * @return the updated task
     */
    public Task updateWithMessage(Message message, Task task) {
        Task.Builder taskBuilder = new Task.Builder(task);
        List<Message> history = task.getHistory();
        if (history == null) {
            history = new ArrayList<>();
        }
        if (task.getStatus().message() != null) {
            history.add(task.getStatus().message());
            taskBuilder.status(new TaskStatus(task.getStatus().state(), null, task.getStatus().timestamp()));
        }
        history.add(message);
        taskBuilder.history(history);
        currentTask = taskBuilder.build();
        return currentTask;
    }

    private void saveTask(Task task) {
        currentTask = task;
        if (taskId == null) {
            taskId = currentTask.getId();
            contextId = currentTask.getContextId();
        }
    }
}