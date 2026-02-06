package io.a2a.grpc.mapper;

import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.TaskStatusUpdateEvent} and {@link io.a2a.grpc.TaskStatusUpdateEvent}.
 * <p>
 * Now fully declarative using Builder pattern with @BeanMapping.
 * Builder's isFinal() method handles the Java "final" keyword mapping.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {TaskStatusMapper.class, A2ACommonFieldMapper.class})
public interface TaskStatusUpdateEventMapper {

    TaskStatusUpdateEventMapper INSTANCE = A2AMappers.getMapper(TaskStatusUpdateEventMapper.class);

    /**
     * Converts domain TaskStatusUpdateEvent to proto.
     * Uses declarative mapping with CommonFieldMapper for metadata conversion.
     * Note: isFinal field is ignored as it has been removed from the proto (field 4 is reserved).
     */
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    io.a2a.grpc.TaskStatusUpdateEvent toProto(TaskStatusUpdateEvent domain);

    /**
     * Converts proto TaskStatusUpdateEvent to domain.
     * Note: isFinal is derived from status.state().isFinal() since the field has been removed from the proto (field 4 is reserved).
     */
    default TaskStatusUpdateEvent fromProto(io.a2a.grpc.TaskStatusUpdateEvent proto) {
        if (proto == null) {
            return null;
        }
        TaskStatus status = TaskStatusMapper.INSTANCE.fromProto(proto.getStatus());
        return new TaskStatusUpdateEvent(
                proto.getTaskId(),
                status,
                proto.getContextId(),
                status != null && status.state() != null && status.state().isFinal(),
                A2ACommonFieldMapper.INSTANCE.metadataFromProto(proto.getMetadata())
        );
    }
}
