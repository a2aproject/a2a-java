package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.TaskStatus;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.TaskStatus} and {@link org.a2aproject.sdk.grpc.TaskStatus}.
 * <p>
 * Handles conversion of task status including state, optional message, and timestamp.
 * Uses TaskStateMapper for state conversion, MessageMapper for message conversion,
 * and CommonFieldMapper for timestamp conversion.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {TaskStateMapper.class, MessageMapper.class, A2ACommonFieldMapper.class})
public interface TaskStatusMapper {

    TaskStatusMapper INSTANCE = A2AMappers.getMapper(TaskStatusMapper.class);

    /**
     * Converts domain TaskStatus to proto TaskStatus.
     * Uses MessageMapper for message and CommonFieldMapper for timestamp conversion.
     */
    @Mapping(target = "state", source = "state", conditionExpression = "java(domain.state() != null)")
    @Mapping(target = "message", source = "message", conditionExpression = "java(domain.message() != null)")
    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "offsetDateTimeToProtoTimestamp")
    org.a2aproject.sdk.grpc.TaskStatus toProto(TaskStatus domain);

    /**
     * Converts proto TaskStatus to domain TaskStatus.
     * Uses MessageMapper for message and CommonFieldMapper for timestamp conversion.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "protoTimestampToOffsetDateTime")
    TaskStatus fromProto(org.a2aproject.sdk.grpc.TaskStatus proto);
}
