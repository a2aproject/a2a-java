package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import io.a2a.spec.Task;

/**
 * A client event containing the complete state of a task.
 * <p>
 * TaskEvent represents a snapshot of a task's full state at a point in time. This event type
 * is typically received in two scenarios:
 * <ol>
 *   <li><b>Final task state:</b> When a task reaches a terminal state (COMPLETED, FAILED, CANCELED),
 *       the agent may send a TaskEvent with the complete final state</li>
 *   <li><b>Non-streaming mode:</b> When streaming is disabled, the client receives a single
 *       TaskEvent containing the final result after the agent completes processing</li>
 * </ol>
 * <p>
 * <b>Contrast with TaskUpdateEvent:</b> While {@link TaskUpdateEvent} provides incremental
 * updates during task execution (status changes, new artifacts), TaskEvent provides the
 * complete task state in a single event.
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * client.addConsumer((event, agentCard) -> {
 *     if (event instanceof TaskEvent te) {
 *         Task task = te.getTask();
 *         
 *         // Check task state
 *         TaskState state = task.status().state();
 *         switch (state) {
 *             case COMPLETED -> {
 *                 // Task finished successfully
 *                 if (task.artifact() != null) {
 *                     System.out.println("Result: " + task.artifact().parts());
 *                 }
 *             }
 *             case FAILED -> {
 *                 // Task failed
 *                 String error = task.status().message();
 *                 System.err.println("Task failed: " + error);
 *             }
 *             case CANCELED -> {
 *                 System.out.println("Task was canceled");
 *             }
 *             default -> {
 *                 System.out.println("Task in state: " + state);
 *             }
 *         }
 *     }
 * });
 * }</pre>
 * <p>
 * <b>Task contents:</b> The contained {@link Task} includes:
 * <ul>
 *   <li><b>id:</b> Unique task identifier</li>
 *   <li><b>status:</b> Current state (SUBMITTED, WORKING, COMPLETED, FAILED, CANCELED, etc.)</li>
 *   <li><b>artifact:</b> Task results (if available)</li>
 *   <li><b>contextId:</b> Associated session/context identifier</li>
 *   <li><b>metadata:</b> Custom task metadata</li>
 *   <li><b>history:</b> Optional state transition history</li>
 * </ul>
 * <p>
 * <b>Terminal states:</b> When a task reaches a final state, no further updates will be
 * received for that task:
 * <ul>
 *   <li>COMPLETED - task finished successfully</li>
 *   <li>FAILED - task encountered an error</li>
 *   <li>CANCELED - task was canceled by user or system</li>
 *   <li>REJECTED - task was rejected (e.g., authorization failure)</li>
 * </ul>
 *
 * @see ClientEvent
 * @see Task
 * @see TaskUpdateEvent
 * @see io.a2a.spec.TaskState
 * @see io.a2a.spec.TaskStatus
 */
public final class TaskEvent implements ClientEvent {

    private final Task task;

    /**
     * Create a task event.
     *
     * @param task the task state received from the agent (required)
     */
    public TaskEvent(Task task) {
        checkNotNullParam("task", task);
        this.task = task;
    }

    /**
     * Get the task contained in this event.
     *
     * @return the complete task state
     */
    public Task getTask() {
        return task;
    }
}
