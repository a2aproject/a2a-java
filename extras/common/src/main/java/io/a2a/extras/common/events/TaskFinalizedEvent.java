package io.a2a.extras.common.events;

/**
 * CDI event fired when a task reaches a final state and is successfully persisted to the database.
 * This event is fired AFTER the database transaction commits, making it safe for downstream
 * components to assume the task is durably stored.
 *
 * <p>Used by the replicated queue manager to send poison pill events after ensuring
 * the final task state is committed to the database, eliminating race conditions.
 */
public class TaskFinalizedEvent {
    private final String taskId;

    public TaskFinalizedEvent(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public String toString() {
        return "TaskFinalizedEvent{taskId='" + taskId + "'}";
    }
}
