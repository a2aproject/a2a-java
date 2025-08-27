package io.a2a.spec;

import static io.a2a.spec.Message.MESSAGE;
import static io.a2a.spec.Task.TASK;
import static io.a2a.spec.TaskArtifactUpdateEvent.ARTIFACT_UPDATE;
import static io.a2a.spec.TaskStatusUpdateEvent.STATUS_UPDATE;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
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

    String getKind();
}
