package io.a2a.spec;

/**
 * Sealed interface for incremental update events during task execution.
 * <p>
 * UpdateEvent represents partial updates to task state or content that can be emitted during
 * streaming operations. These events enable real-time progress tracking and incremental result
 * delivery without waiting for complete task finalization.
 * <p>
 * Permitted implementations:
 * <ul>
 *   <li>{@link TaskStatusUpdateEvent} - Updates to task status and state transitions</li>
 *   <li>{@link TaskArtifactUpdateEvent} - Updates to task artifacts (creation, modification, appending)</li>
 * </ul>
 * <p>
 * Update events are always part of {@link StreamingEventKind} and provide granular visibility
 * into agent progress during long-running operations.
 *
 * @see StreamingEventKind
 * @see TaskStatusUpdateEvent
 * @see TaskArtifactUpdateEvent
 */
public sealed interface UpdateEvent permits TaskStatusUpdateEvent, TaskArtifactUpdateEvent {
    String getContextId();

    String getTaskId();
}
