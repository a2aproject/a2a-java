package org.a2aproject.sdk.compat03.spec;

/**
 * Interface for events that can be returned from non-streaming A2A Protocol operations.
 * <p>
 * EventKind represents events that are suitable for synchronous request-response patterns.
 * These events provide complete state information and are typically returned as the final
 * result of an operation.
 * <p>
 * EventKind implementations use polymorphic JSON serialization with the "kind" discriminator
 * to determine the concrete type during deserialization.
 * <p>
 * Permitted implementations:
 * <ul>
 *   <li>{@link Task_v0_3} - Complete task state with status and artifacts</li>
 *   <li>{@link Message_v0_3} - Full message with all content parts</li>
 * </ul>
 *
 * @see StreamingEventKind_v0_3
 * @see Event_v0_3
 */
public interface EventKind_v0_3 {

    String getKind();
}
