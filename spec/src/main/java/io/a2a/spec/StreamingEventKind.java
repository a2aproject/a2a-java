package io.a2a.spec;

import static io.a2a.spec.Message.MESSAGE;
import static io.a2a.spec.Task.TASK;
import static io.a2a.spec.TaskArtifactUpdateEvent.ARTIFACT_UPDATE;
import static io.a2a.spec.TaskStatusUpdateEvent.STATUS_UPDATE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed interface for events that can be streamed in the A2A protocol.
 * 
 * This interface extends {@link Event} and provides polymorphic JSON serialization/deserialization
 * support for streaming events in the A2A framework. The "kind" property is used as a discriminator
 * to determine the concrete type during JSON processing in streaming contexts.
 * 
 * Currently supported streaming event kinds include:
 * <ul>
 * <li>{@code "task"} - Represents a {@link Task} object</li>
 * <li>{@code "message"} - Represents a {@link Message} object</li>
 * <li>{@code "status-update"} - Represents a {@link TaskStatusUpdateEvent}</li>
 * <li>{@code "artifact-update"} - Represents a {@link TaskArtifactUpdateEvent}</li>
 * </ul>
 * 
 * The sealed interface restricts implementations to the explicitly permitted classes,
 * ensuring type safety and enabling exhaustive pattern matching in switch expressions.
 * 
 * The Jackson annotations configure automatic type resolution based on the "kind" field
 * in JSON data, enabling seamless conversion between JSON and the appropriate Java types
 * during streaming operations.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Task.class, name = TASK),
        @JsonSubTypes.Type(value = Message.class, name = MESSAGE),
        @JsonSubTypes.Type(value = TaskStatusUpdateEvent.class, name = STATUS_UPDATE),
        @JsonSubTypes.Type(value = TaskArtifactUpdateEvent.class, name = ARTIFACT_UPDATE)
})
public sealed interface StreamingEventKind extends Event permits Task, Message, TaskStatusUpdateEvent, TaskArtifactUpdateEvent {

    /**
     * Returns the kind/type identifier for this streaming event.
     * 
     * This method provides the discriminator value used for JSON serialization
     * and type resolution in streaming contexts. The returned string should match
     * one of the values defined in the {@link JsonSubTypes} annotation.
     * 
     * @return the kind identifier (e.g., "task", "message", "status-update", "artifact-update")
     */
    String getKind();
}
