package org.a2aproject.sdk.compat03.client;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.appendArtifactToTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.A2AClientError_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientInvalidArgsError_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientInvalidStateError_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.jspecify.annotations.Nullable;

/**
 * Helps manage a task's lifecycle during the execution of a request.
 * Responsible for retrieving, saving, and updating the task based on
 * events received from the agent.
 */
class ClientTaskManager_v0_3 {

    private @Nullable Task_v0_3 currentTask;
    private @Nullable String taskId;
    private @Nullable String contextId;

    ClientTaskManager_v0_3() {
        this.currentTask = null;
        this.taskId = null;
        this.contextId = null;
    }

    Task_v0_3 getCurrentTask() throws A2AClientInvalidStateError_v0_3 {
        if (currentTask == null) {
            throw new A2AClientInvalidStateError_v0_3("No current task");
        }
        return currentTask;
    }

    Task_v0_3 saveTaskEvent(Task_v0_3 task) throws A2AClientInvalidArgsError_v0_3 {
        if (currentTask != null && !currentTask.id().equals(task.id())) {
            throw new A2AClientInvalidArgsError_v0_3("Task is already set, create new manager for new tasks.");
        }
        saveTask(task);
        return task;
    }

    Task_v0_3 saveTaskEvent(TaskStatusUpdateEvent_v0_3 taskStatusUpdateEvent) throws A2AClientError_v0_3 {
        if (taskId == null) {
            taskId = taskStatusUpdateEvent.taskId();
        }
        if (contextId == null) {
            contextId = taskStatusUpdateEvent.contextId();
        }
        Task_v0_3 task = currentTask;
        if (task == null) {
            task = new Task_v0_3.Builder()
                    .status(new TaskStatus_v0_3(TaskState_v0_3.UNKNOWN))
                    .id(taskId)
                    .contextId(contextId == null ? "" : contextId)
                    .build();
        }

        Task_v0_3.Builder taskBuilder = new Task_v0_3.Builder(task);
        if (taskStatusUpdateEvent.status().message() != null) {
            List<Message_v0_3> history = new ArrayList<>(task.history());
            history.add(taskStatusUpdateEvent.status().message());
            taskBuilder.history(history);
        }
        if (taskStatusUpdateEvent.metadata() != null) {
            Map<String, Object> newMetadata = task.metadata() != null ? new HashMap<>(task.metadata()) : new HashMap<>();
            newMetadata.putAll(taskStatusUpdateEvent.metadata());
            taskBuilder.metadata(newMetadata);
        }
        taskBuilder.status(taskStatusUpdateEvent.status());
        currentTask = taskBuilder.build();
        return currentTask;
    }

    Task_v0_3 saveTaskEvent(TaskArtifactUpdateEvent_v0_3 taskArtifactUpdateEvent) {
        if (taskId == null) {
            taskId = taskArtifactUpdateEvent.taskId();
        }
        if (contextId == null) {
            contextId = taskArtifactUpdateEvent.contextId();
        }
        Task_v0_3 task = currentTask;
        if (task == null) {
            task = new Task_v0_3.Builder()
                    .status(new TaskStatus_v0_3(TaskState_v0_3.UNKNOWN))
                    .id(taskId)
                    .contextId(contextId == null ? "" : contextId)
                    .build();
        }
        currentTask = appendArtifactToTask(task, taskArtifactUpdateEvent, taskId);
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
    Task_v0_3 updateWithMessage(Message_v0_3 message, Task_v0_3 task) {
        Task_v0_3.Builder taskBuilder = new Task_v0_3.Builder(task);
        List<Message_v0_3> history = new ArrayList<>(task.history());
        if (task.status().message() != null) {
            history.add(task.status().message());
            taskBuilder.status(new TaskStatus_v0_3(task.status().state(), null, task.status().timestamp()));
        }
        history.add(message);
        taskBuilder.history(history);
        currentTask = taskBuilder.build();
        return currentTask;
    }

    private void saveTask(Task_v0_3 task) {
        currentTask = task;
        if (taskId == null) {
            taskId = currentTask.id();
            contextId = currentTask.contextId();
        }
    }
}