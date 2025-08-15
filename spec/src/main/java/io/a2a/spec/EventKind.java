package io.a2a.spec;

import static io.a2a.spec.Message.MESSAGE;
import static io.a2a.spec.Task.TASK;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for objects that can be categorized by a specific "kind" in the A2A protocol.
 * 
 * This interface provides polymorphic JSON serialization/deserialization support
 * for different types of events and objects in the A2A framework. The "kind" property
 * is used as a discriminator to determine the concrete type during JSON processing.
 * 
 * Currently supported kinds include:
 * <ul>
 * <li>{@code "task"} - Represents a {@link Task} object</li>
 * <li>{@code "message"} - Represents a {@link Message} object</li>
 * </ul>
 * 
 * The Jackson annotations configure automatic type resolution based on the "kind" field
 * in JSON data, enabling seamless conversion between JSON and the appropriate Java types.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Task.class, name = TASK),
        @JsonSubTypes.Type(value = Message.class, name = MESSAGE)
})
public interface EventKind {

    /**
     * Returns the kind/type identifier for this object.
     * 
     * This method provides the discriminator value used for JSON serialization
     * and type resolution. The returned string should match one of the values
     * defined in the {@link JsonSubTypes} annotation.
     * 
     * @return the kind identifier (e.g., "task", "message")
     */
    String getKind();
}
