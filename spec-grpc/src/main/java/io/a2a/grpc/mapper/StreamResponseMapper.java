package io.a2a.grpc.mapper;

import io.a2a.spec.Message;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.StreamingEventKind} and {@link io.a2a.grpc.StreamResponse}.
 * <p>
 * StreamResponse uses a protobuf oneof field to represent polymorphic streaming events.
 * StreamingEventKind is a sealed interface with four permitted implementations:
 * <ul>
 *   <li>{@link Task} - Complete task state</li>
 *   <li>{@link Message} - Full message</li>
 *   <li>{@link TaskStatusUpdateEvent} - Status update event</li>
 *   <li>{@link TaskArtifactUpdateEvent} - Artifact update event</li>
 * </ul>
 * <p>
 * This mapper provides bidirectional conversion using instanceof checks (toProto)
 * and switch expressions on the oneof case (fromProto).
 */
@Mapper(config = A2AProtoMapperConfig.class,
        uses = {TaskMapper.class, MessageMapper.class, TaskStatusUpdateEventMapper.class, TaskArtifactUpdateEventMapper.class})
public interface StreamResponseMapper {

    StreamResponseMapper INSTANCE = A2AMappers.getMapper(StreamResponseMapper.class);

    /**
     * Converts domain StreamingEventKind to proto StreamResponse.
     * Uses instanceof checks to determine which oneof field to set.
     *
     * @param domain the streaming event kind (Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent)
     * @return the proto StreamResponse with the appropriate oneof field set
     */
    default io.a2a.grpc.StreamResponse toProto(StreamingEventKind domain) {
        if (domain == null) {
            return null;
        }

        if (domain instanceof Task task) {
            return io.a2a.grpc.StreamResponse.newBuilder()
                    .setTask(TaskMapper.INSTANCE.toProto(task))
                    .build();
        } else if (domain instanceof Message message) {
            return io.a2a.grpc.StreamResponse.newBuilder()
                    .setMsg(MessageMapper.INSTANCE.toProto(message))
                    .build();
        } else if (domain instanceof TaskStatusUpdateEvent statusUpdate) {
            return io.a2a.grpc.StreamResponse.newBuilder()
                    .setStatusUpdate(TaskStatusUpdateEventMapper.INSTANCE.toProto(statusUpdate))
                    .build();
        } else if (domain instanceof TaskArtifactUpdateEvent artifactUpdate) {
            return io.a2a.grpc.StreamResponse.newBuilder()
                    .setArtifactUpdate(TaskArtifactUpdateEventMapper.INSTANCE.toProto(artifactUpdate))
                    .build();
        }

        throw new IllegalArgumentException("Unknown StreamingEventKind type: " + domain.getClass().getName());
    }

    /**
     * Converts proto StreamResponse to domain StreamingEventKind.
     * Uses switch expression on the oneof case to determine which type to return.
     *
     * @param proto the proto StreamResponse
     * @return the corresponding domain streaming event kind
     * @throws IllegalArgumentException if the oneof field is not set
     */
    default StreamingEventKind fromProto(io.a2a.grpc.StreamResponse proto) {
        if (proto == null) {
            return null;
        }

        return switch (proto.getPayloadCase()) {
            case TASK ->
                TaskMapper.INSTANCE.fromProto(proto.getTask());
            case MSG ->
                MessageMapper.INSTANCE.fromProto(proto.getMsg());
            case STATUS_UPDATE ->
                TaskStatusUpdateEventMapper.INSTANCE.fromProto(proto.getStatusUpdate());
            case ARTIFACT_UPDATE ->
                TaskArtifactUpdateEventMapper.INSTANCE.fromProto(proto.getArtifactUpdate());
            case PAYLOAD_NOT_SET ->
                throw new IllegalArgumentException("StreamResponse payload oneof field not set");
        };
    }
}
