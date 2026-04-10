package org.a2aproject.sdk.compat03.spec;

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
 *   <li>{@link Task_v0_3} - Complete task state (typically the final event in a stream)</li>
 *   <li>{@link Message_v0_3} - Full message (complete message in the stream)</li>
 *   <li>{@link TaskStatusUpdateEvent_v0_3} - Incremental status updates (e.g., SUBMITTED → WORKING → COMPLETED)</li>
 *   <li>{@link TaskArtifactUpdateEvent_v0_3} - Incremental artifact updates (partial results, chunks)</li>
 * </ul>
 * <p>
 * Streaming events enable patterns like:
 * <ul>
 *   <li>Progressive text generation (chunks of response as generated)</li>
 *   <li>Status notifications (task state transitions)</li>
 *   <li>Partial results (early artifacts before task completion)</li>
 * </ul>
 *
 * @see Event_v0_3
 * @see EventKind_v0_3
 * @see UpdateEvent_v0_3
 */
public sealed interface StreamingEventKind_v0_3 extends Event_v0_3 permits Task_v0_3, Message_v0_3, TaskStatusUpdateEvent_v0_3, TaskArtifactUpdateEvent_v0_3 {

    String getKind();
}
