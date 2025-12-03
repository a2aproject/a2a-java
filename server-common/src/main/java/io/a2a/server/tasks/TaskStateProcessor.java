package io.a2a.server.tasks;

import static io.a2a.spec.TaskState.SUBMITTED;
import static io.a2a.util.Utils.appendArtifactToTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TaskStateProcessor processes events to build task state without persistence.
 * This class maintains a collection of all tasks that are handled by a RequestHandler and applies events to them,
 * separating state building from persistence concerns.
 */
public class TaskStateProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStateProcessor.class);

    // key is the task ID
    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

    /**
     * Creates a TaskStateProcessor as a singleton service.
     */
    public TaskStateProcessor() {
    }

    /**
     * Processes an event and updates the internal task state.
     *
     * @param event the event to process
     * @param initialMessage the initial message to use if creating a new task (may be null)
     * @return the updated task state
     */
    public Task processEvent(Event event, Message initialMessage) {
        if (event instanceof Task task) {
            tasks.put(task.getId(), task);
            return task;
        } else if (event instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
            return processTaskStatusUpdate(taskStatusUpdateEvent, initialMessage);
        } else if (event instanceof TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
            return processTaskArtifactUpdate(taskArtifactUpdateEvent, initialMessage);
        }
        // Unknown event type - return null
        LOGGER.warn("Unknown event type: {}", event.getClass().getName());
        return null;
    }

    /**
     * Adds a message to the task's history.
     *
     * @param taskId the task ID
     * @param message the message to add
     * @return the updated task
     */
    public Task addMessageToHistory(String taskId, Message message) {
        Task task = tasks.get(taskId);
        if (task == null) {
            LOGGER.warn("Cannot add message to history - task {} not found", taskId);
            return null;
        }

        // FIXME manipulation & update of Task could be provide by methods on the Task class
        List<Message> history = new ArrayList<>(task.getHistory());

        TaskStatus status = task.getStatus();
        if (status.message() != null) {
            history.add(status.message());
            status = new TaskStatus(status.state(), null, status.timestamp());
        }
        history.add(message);
        task = new Task.Builder(task)
                .status(status)
                .history(history)
                .build();
        tasks.put(task.getId(), task);
        return task;
    }

    /**
     * Gets a specific task by ID.
     *
     * @param taskId the task ID
     * @return the task, or null if not found or if taskId is null
     */
    public Task getTask(String taskId) {
        if (taskId == null) {
            return null;
        }
        return tasks.get(taskId);
    }

    /**
     * Sets a task in the processor (e.g., when loading from TaskStore).
     *
     * @param task the task to set
     */
    public void setTask(Task task) {
        if (task != null) {
            tasks.put(task.getId(), task);
        }
    }

    /**
     * Removes a task from the processor (e.g., after final persistence).
     *
     * @param taskId the task ID to remove
     */
    public void removeTask(String taskId) {
        tasks.remove(taskId);
    }

    /**
     * Processes a TaskStatusUpdateEvent.
     */
    private Task processTaskStatusUpdate(TaskStatusUpdateEvent event, Message initialMessage) {
        Task task = ensureTask(event.getTaskId(), event.getContextId(), initialMessage);

        Task.Builder builder = new Task.Builder(task)
                .status(event.getStatus());

        // FIXME manipulation & update of Task could be provide by methods on the Task class
        if (task.getStatus().message() != null) {
            List<Message> newHistory = task.getHistory() == null ? new ArrayList<>() : new ArrayList<>(task.getHistory());
            newHistory.add(task.getStatus().message());
            builder.history(newHistory);
        }

        // Handle metadata from the event
        if (event.getMetadata() != null) {
            Map<String, Object> metadata = task.getMetadata() == null ? new HashMap<>() : new HashMap<>(task.getMetadata());
            metadata.putAll(event.getMetadata());
            builder.metadata(metadata);
        }

        task = builder.build();
        tasks.put(task.getId(), task);
        return task;
    }

    /**
     * Processes a TaskArtifactUpdateEvent.
     */
    private Task processTaskArtifactUpdate(TaskArtifactUpdateEvent event, Message initialMessage) {
        Task task = ensureTask(event.getTaskId(), event.getContextId(), initialMessage);
        task = appendArtifactToTask(task, event);
        tasks.put(task.getId(), task);
        return task;
    }

    /**
     * Ensures a task exists in the processor, creating one if necessary.
     */
    private Task ensureTask(String taskId, String contextId, Message initialMessage) {
        Task task = tasks.get(taskId);
        if (task != null) {
            return task;
        }
        // Create a new task
        task = createTask(taskId, contextId, initialMessage);
        tasks.put(task.getId(), task);
        return task;
    }

    /**
     * Creates a new task with the given parameters.
     */
    private Task createTask(String taskId, String contextId, Message initialMessage) {
        List<Message> history = initialMessage != null ? List.of(initialMessage) : null;
        return new Task.Builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(SUBMITTED))
                .history(history)
                .build();
    }
}
