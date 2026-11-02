package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.TaskStatusUpdateEvent} and {@link org.a2aproject.sdk.grpc.TaskStatusUpdateEvent}.
 */
@Mapper(
        config = A2AProtoMapperConfig.class,
        uses = {TaskStatusMapper.class, A2ACommonFieldMapper.class})
public interface TaskStatusUpdateEventMapper {

    TaskStatusUpdateEventMapper INSTANCE = A2AMappers.getMapper(TaskStatusUpdateEventMapper.class);

    /**
     * Converts domain TaskStatusUpdateEvent to proto.
     */
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    org.a2aproject.sdk.grpc.TaskStatusUpdateEvent toProto(TaskStatusUpdateEvent domain);

    /**
     * Converts proto TaskStatusUpdateEvent to domain.
     */
    default TaskStatusUpdateEvent fromProto(org.a2aproject.sdk.grpc.TaskStatusUpdateEvent proto) {
        if (proto == null) {
            return null;
        }
        TaskStatus status = TaskStatusMapper.INSTANCE.fromProto(proto.getStatus());
        return new TaskStatusUpdateEvent(
                proto.getTaskId(),
                status,
                proto.getContextId(),
                A2ACommonFieldMapper.INSTANCE.metadataFromProto(proto.getMetadata()));
    }
}
