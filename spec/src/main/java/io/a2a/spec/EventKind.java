package io.a2a.spec;

import static io.a2a.spec.Message.MESSAGE;
import static io.a2a.spec.Task.TASK;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
 *   <li>{@link Task} - Complete task state with status and artifacts</li>
 *   <li>{@link Message} - Full message with all content parts</li>
 * </ul>
 *
 * @see StreamingEventKind
 * @see Event
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "kind",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Task.class, name = TASK),
        @JsonSubTypes.Type(value = Message.class, name = MESSAGE)
})
public interface EventKind {

    /**
     * Returns the kind identifier for this event.
     *
     * @return the event kind string (e.g., "task", "message")
     */
    String getKind();

    String getContextId();
}
