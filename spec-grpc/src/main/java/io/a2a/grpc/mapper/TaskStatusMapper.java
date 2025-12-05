package io.a2a.grpc.mapper;

import io.a2a.spec.TaskStatus;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.TaskStatus} and {@link io.a2a.grpc.TaskStatus}.
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
    io.a2a.grpc.TaskStatus toProto(TaskStatus domain);

    /**
     * Converts proto TaskStatus to domain TaskStatus.
     * Uses MessageMapper for message and CommonFieldMapper for timestamp conversion.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "protoTimestampToOffsetDateTime")
    TaskStatus fromProto(io.a2a.grpc.TaskStatus proto);
}
