package org.a2aproject.sdk.extras.common.events;

/**
 * CDI event fired when a task reaches a final state and is successfully persisted to the database.
 * This event is fired AFTER the database transaction commits, making it safe for downstream
 * components to assume the task is durably stored.
 *
 * <p>Used by the replicated queue manager to send the final task state before the poison pill,
 * ensuring correct event ordering across instances and eliminating race conditions.
 */
public class TaskFinalizedEvent {
    private final String taskId;
    private final Object task;  // Task type from org.a2aproject.sdk.spec - using Object to avoid dependency

    public TaskFinalizedEvent(String taskId, Object task) {
        this.taskId = taskId;
        this.task = task;
    }

    public String getTaskId() {
        return taskId;
    }

    public Object getTask() {
        return task;
    }

    @Override
    public String toString() {
        return "TaskFinalizedEvent{taskId='" + taskId + "', task=" + task + "}";
    }
}
