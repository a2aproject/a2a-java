package io.a2a.spec;

/**
 * Interface for events that can be returned from non-streaming A2A Protocol operations.
 * <p>
 * EventKind represents events that are suitable for synchronous request-response patterns.
 * These events provide complete state information and are typically returned as the final
 * result of an operation.
 * <p>
 * EventKind implementations use polymorphic JSON serialization where the JSON member name
 * itself acts as the type discriminator (e.g., "task", "message").
 * <p>
 * Permitted implementations:
 * <ul>
 *   <li>{@link Task} - Complete task state with status and artifacts</li>
 *   <li>{@link Message} - Full message with all content parts</li>
 * </ul>
 *
 * @see StreamingEventKind
 * @see Event
 */
public interface EventKind {

    /**
     * Returns the type identifier for this event.
     * <p>
     * This method provides programmatic type discrimination for routing, logging, and debugging.
     * <strong>NOTE:</strong> This value is NOT serialized to JSON in protocol v1.0+.
     *
     * @return the event kind string (e.g., "task", "message")
     */
    String kind();
}
