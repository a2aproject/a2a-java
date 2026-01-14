package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import io.a2a.spec.Task;
import io.a2a.spec.UpdateEvent;

/**
 * A client event containing an incremental update to a task.
 * <p>
 * TaskUpdateEvent represents a change to a task's state during execution. It provides both
 * the current complete task state and the specific update that triggered this event. This
 * event type is the primary mechanism for tracking task progress in streaming scenarios.
 * <p>
 * <b>Two types of updates:</b>
 * <ul>
 *   <li>{@link io.a2a.spec.TaskStatusUpdateEvent} - task state changed (e.g., SUBMITTED → WORKING → COMPLETED)</li>
 *   <li>{@link io.a2a.spec.TaskArtifactUpdateEvent} - new content/results available</li>
 * </ul>
 * <p>
 * <b>Streaming task lifecycle example:</b>
 * <pre>{@code
 * client.sendMessage(A2A.toUserMessage("Summarize this document"));
 *
 * // Client receives sequence of TaskUpdateEvents:
 * 1. TaskUpdateEvent(task=Task[status=SUBMITTED], updateEvent=TaskStatusUpdateEvent)
 * 2. TaskUpdateEvent(task=Task[status=WORKING], updateEvent=TaskStatusUpdateEvent)
 * 3. TaskUpdateEvent(task=Task[status=WORKING, artifact=[partial]], updateEvent=TaskArtifactUpdateEvent)
 * 4. TaskUpdateEvent(task=Task[status=WORKING, artifact=[more content]], updateEvent=TaskArtifactUpdateEvent)
 * 5. TaskUpdateEvent(task=Task[status=COMPLETED, artifact=[final]], updateEvent=TaskStatusUpdateEvent)
 * }</pre>
 * <p>
 * <b>Example usage - tracking progress:</b>
 * <pre>{@code
 * client.addConsumer((event, agentCard) -> {
 *     if (event instanceof TaskUpdateEvent tue) {
 *         Task currentTask = tue.getTask();
 *         UpdateEvent update = tue.getUpdateEvent();
 *         
 *         // Handle status changes
 *         if (update instanceof TaskStatusUpdateEvent statusUpdate) {
 *             TaskState newState = currentTask.status().state();
 *             System.out.println("Task " + currentTask.id() + " → " + newState);
 *             
 *             if (newState == TaskState.COMPLETED) {
 *                 System.out.println("Final result: " +
 *                     currentTask.artifact().parts());
 *             } else if (newState == TaskState.FAILED) {
 *                 System.err.println("Error: " +
 *                     currentTask.status().message());
 *             }
 *         }
 *         
 *         // Handle new content
 *         if (update instanceof TaskArtifactUpdateEvent artifactUpdate) {
 *             Artifact newContent = artifactUpdate.artifact();
 *             System.out.println("New content received: " + newContent.parts());
 *             
 *             // For streaming text generation
 *             newContent.parts().stream()
 *                 .filter(p -> p instanceof TextPart)
 *                 .map(p -> ((TextPart) p).text())
 *                 .forEach(System.out::print);  // Print incrementally
 *         }
 *     }
 * });
 * }</pre>
 * <p>
 * <b>Reconstructing complete state:</b> The {@link #getTask()} method returns the task with
 * all updates applied up to this point. The client automatically maintains the complete
 * task state by merging updates, so consumers don't need to manually track changes:
 * <pre>{@code
 * // Each TaskUpdateEvent contains the fully updated task
 * TaskUpdateEvent event1 // task has status=WORKING, artifact=null
 * TaskUpdateEvent event2 // task has status=WORKING, artifact=[chunk1]
 * TaskUpdateEvent event3 // task has status=WORKING, artifact=[chunk1, chunk2]
 * TaskUpdateEvent event4 // task has status=COMPLETED, artifact=[chunk1, chunk2, final]
 * }</pre>
 * <p>
 * <b>Artifact updates:</b> When {@link io.a2a.spec.TaskArtifactUpdateEvent} is received,
 * the artifact may be:
 * <ul>
 *   <li><b>Incremental:</b> New parts appended to existing artifact (common for streaming text)</li>
 *   <li><b>Replacement:</b> Entire artifact replaced (less common)</li>
 * </ul>
 * The {@link #getTask()} always reflects the current complete artifact state.
 * <p>
 * <b>Status transitions:</b> Common task state transitions:
 * <pre>
 * SUBMITTED → WORKING → COMPLETED
 * SUBMITTED → WORKING → FAILED
 * SUBMITTED → WORKING → CANCELED
 * SUBMITTED → AUTH_REQUIRED → (waiting for auth) → WORKING → COMPLETED
 * </pre>
 *
 * @see ClientEvent
 * @see Task
 * @see io.a2a.spec.UpdateEvent
 * @see io.a2a.spec.TaskStatusUpdateEvent
 * @see io.a2a.spec.TaskArtifactUpdateEvent
 * @see io.a2a.spec.TaskState
 */
public final class TaskUpdateEvent implements ClientEvent {

    private final Task task;
    private final UpdateEvent updateEvent;

    /**
     * Create a task update event.
     * <p>
     * This constructor is typically called internally by the client framework when processing
     * update events from the agent. The {@code task} parameter contains the complete current
     * state with all updates applied, while {@code updateEvent} contains the specific change
     * that triggered this event.
     *
     * @param task the current complete task state with all updates applied (required)
     * @param updateEvent the specific update that triggered this event (required)
     */
    public TaskUpdateEvent(Task task, UpdateEvent updateEvent) {
        checkNotNullParam("task", task);
        checkNotNullParam("updateEvent", updateEvent);
        this.task = task;
        this.updateEvent = updateEvent;
    }

    /**
     * Get the current complete task state.
     * <p>
     * The returned task reflects all updates received up to this point, including the
     * update contained in this event. Consumers can use this method to access the
     * complete current state without manually tracking changes.
     *
     * @return the task with all updates applied
     */
    public Task getTask() {
        return task;
    }

    /**
     * Get the specific update that triggered this event.
     * <p>
     * This will be either:
     * <ul>
     *   <li>{@link io.a2a.spec.TaskStatusUpdateEvent} - indicates a state transition</li>
     *   <li>{@link io.a2a.spec.TaskArtifactUpdateEvent} - indicates new content available</li>
     * </ul>
     *
     * @return the update event
     */
    public UpdateEvent getUpdateEvent() {
        return updateEvent;
    }

}
