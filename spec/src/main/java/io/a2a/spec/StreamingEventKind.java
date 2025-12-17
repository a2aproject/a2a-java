package io.a2a.spec;

/**
 * Sealed interface for events that can be emitted during streaming A2A Protocol operations.
 * <p>
 * StreamingEventKind represents events suitable for asynchronous, progressive updates during
 * agent task execution. Streaming allows agents to provide incremental feedback as work progresses,
 * rather than waiting until task completion.
 * <p>
 * Streaming events use polymorphic JSON serialization with the "kind" discriminator to determine
 * the concrete type during deserialization.
 * <p>
 * Permitted implementations:
 * <ul>
 *   <li>{@link Task} - Complete task state (typically the final event in a stream)</li>
 *   <li>{@link Message} - Full message (complete message in the stream)</li>
 *   <li>{@link TaskStatusUpdateEvent} - Incremental status updates (e.g., SUBMITTED → WORKING → COMPLETED)</li>
 *   <li>{@link TaskArtifactUpdateEvent} - Incremental artifact updates (partial results, chunks)</li>
 * </ul>
 * <p>
 * Streaming events enable patterns like:
 * <ul>
 *   <li>Progressive text generation (chunks of response as generated)</li>
 *   <li>Status notifications (task state transitions)</li>
 *   <li>Partial results (early artifacts before task completion)</li>
 * </ul>
 *
 * @see Event
 * @see EventKind
 * @see UpdateEvent
 */
public sealed interface StreamingEventKind extends Event permits Task, Message, TaskStatusUpdateEvent, TaskArtifactUpdateEvent {

    /**
     * Returns the kind identifier for this streaming event.
     *
     * @return the event kind string (e.g., "task", "message", "status-update", "artifact-update")
     */
    String kind();
}
