package io.a2a.server.tasks;

import static io.a2a.util.Assert.checkNotNullParam;

import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);

    private final TaskStore taskStore;
    private final TaskStateProcessor stateProcessor;
    private String taskId;
    private String contextId;
    private final Message initialMessage;

    public TaskManager(String taskId, String contextId, TaskStore taskStore, TaskStateProcessor stateProcessor, Message initialMessage) {
        checkNotNullParam("taskStore", taskStore);
        checkNotNullParam("stateProcessor", stateProcessor);
        this.taskStore = taskStore;
        this.stateProcessor = stateProcessor;
        this.taskId = taskId;
        this.contextId = contextId;
        this.initialMessage = initialMessage;

        // Load existing task from store if it exists
        if (taskId != null) {
            Task existingTask = taskStore.get(taskId);
            if (existingTask != null) {
                stateProcessor.setTask(existingTask);
            }
        }
    }

    String getTaskId() {
        Task task = stateProcessor.getTask(taskId);
        return task != null ? task.getId() : taskId;
    }

    String getContextId() {
        Task task = stateProcessor.getTask(taskId);
        return task != null ? task.getContextId() : contextId;
    }

    public Task getTask() {
        Task task = stateProcessor.getTask(taskId);
        // If we don't have a task in the processor yet, try loading from store
        if (task == null && taskId != null) {
            task = taskStore.get(taskId);
            if (task != null) {
                stateProcessor.setTask(task);
            }
        }
        return task;
    }

    /**
     * Processes an event to build the updated task state WITHOUT persisting.
     * This separates state building from persistence, allowing callers to
     * decide when to persist the task.
     *
     * @param event the event to process
     * @return the updated task state (not yet persisted)
     * @throws A2AServerException if the event contains invalid data
     */
    public Task processEvent(Event event) throws A2AServerException {
        String eventTaskId = extractTaskId(event);
        String eventContextId = extractContextId(event);

        if (eventTaskId != null) {
            checkIdsAndUpdateIfNecessary(eventTaskId, eventContextId);
        }

        // Ensure we have the latest task from the store before processing the event
        // This is important for events that update existing tasks
        getTask();

        return stateProcessor.processEvent(event, initialMessage);
    }

    /**
     * Processes an event and immediately persists the resulting task state.
     * This is a convenience method that combines processEvent() and saveTask().
     *
     * @param event the event to process
     * @return the persisted task
     * @throws A2AServerException if the event contains invalid data
     */
    public Task processAndSave(Event event) throws A2AServerException {
        Task task = processEvent(event);
        return saveTask(task);
    }

    /**
     * Extracts the task ID from an event.
     */
    private String extractTaskId(Event event) {
        if (event instanceof Task task) {
            return task.getId();
        } else if (event instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
            return taskStatusUpdateEvent.getTaskId();
        } else if (event instanceof TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
            return taskArtifactUpdateEvent.getTaskId();
        }
        return null;
    }

    /**
     * Extracts the context ID from an event.
     */
    private String extractContextId(Event event) {
        if (event instanceof Task task) {
            return task.getContextId();
        } else if (event instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
            return taskStatusUpdateEvent.getContextId();
        } else if (event instanceof TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
            return taskArtifactUpdateEvent.getContextId();
        }
        return null;
    }

    public Task updateWithMessage(Message message, Task task) {
        task = stateProcessor.addMessageToHistory(task.getId(), message);
        saveTask(task);
        return task;
    }

    private void checkIdsAndUpdateIfNecessary(String eventTaskId, String eventContextId) throws A2AServerException {
        if (taskId != null && !eventTaskId.equals(taskId)) {
            throw new A2AServerException(
                    "Invalid task id",
                    new InvalidParamsError(String.format("Task in event doesn't match TaskManager ")));
        }
        // Update taskId and contextId if they were null
        if (taskId == null) {
            taskId = eventTaskId;
        }
        if (contextId == null) {
            contextId = eventContextId;
        }
    }

    /**
     * Persists a task to the TaskStore.
     *
     * @param task the task to save
     * @return the saved task
     */
    public Task saveTask(Task task) {
        if (task == null) {
            return null;
        }
        taskStore.save(task);
        // Ensure the task is in the state processor
        stateProcessor.setTask(task);
        return task;
    }
}
