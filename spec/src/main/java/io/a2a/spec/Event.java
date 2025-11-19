package io.a2a.spec;

/**
 * Marker interface for all event types in the A2A Protocol.
 * <p>
 * Events are the fundamental communication mechanism for agent task execution and state updates.
 * They enable both synchronous request-response patterns and asynchronous streaming updates.
 * <p>
 * The Event hierarchy includes:
 * <ul>
 *   <li>{@link EventKind} - Events that can be returned from non-streaming operations</li>
 *   <li>{@link StreamingEventKind} - Events that can be emitted during streaming responses</li>
 *   <li>{@link UpdateEvent} - Incremental update events for task progress</li>
 * </ul>
 * <p>
 * Common event types include:
 * <ul>
 *   <li>{@link Task} - Complete task state</li>
 *   <li>{@link Message} - Message exchange</li>
 *   <li>{@link TaskStatusUpdateEvent} - Task status changes</li>
 *   <li>{@link TaskArtifactUpdateEvent} - Artifact creation/updates</li>
 * </ul>
 *
 * @see EventKind
 * @see StreamingEventKind
 * @see UpdateEvent
 */
public interface Event {
}
