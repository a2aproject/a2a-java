package io.a2a.spec;

/**
 * Sealed interface for events that can be emitted during streaming A2A Protocol operations.
 * <p>
 * StreamingEventKind represents events suitable for asynchronous, progressive updates during
 * agent task execution. Streaming allows agents to provide incremental feedback as work progresses,
 * rather than waiting until task completion.
 * <p>
 * Streaming events use polymorphic JSON serialization where the JSON member name itself acts as
 * the type discriminator (e.g., "task", "message", "statusUpdate", "artifactUpdate").
 * This aligns with Protocol Buffers' oneof semantics and modern API design practices.
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
     * Returns the type identifier for this streaming event.
     * <p>
     * This method provides programmatic type discrimination for routing, logging, and debugging.
     * <strong>NOTE:</strong> This value is NOT serialized to JSON in protocol v1.0+.
     * For JSON serialization, the wrapper field name serves as the discriminator.
     * <p>
     * Use {@code instanceof} pattern matching to determine the concrete event type:
     * <pre>{@code
     * if (event instanceof Task task) {
     *     // Handle task
     * } else if (event instanceof TaskStatusUpdateEvent statusUpdate) {
     *     // Handle status update
     * }
     * }</pre>
     *
     * @return the event type identifier (e.g., "task", "message", "status-update", "artifact-update")
     */
    String kind();
}
