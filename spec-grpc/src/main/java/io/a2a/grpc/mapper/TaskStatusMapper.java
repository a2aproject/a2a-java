package io.a2a.grpc.mapper;

import io.a2a.spec.TaskStatus;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.TaskStatus} and {@link io.a2a.grpc.TaskStatus}.
 * <p>
 * Handles conversion of task status including state, optional message, and timestamp.
 * Uses TaskStateMapper for state conversion, MessageMapper for message conversion,
 * and CommonFieldMapper for timestamp conversion.
 */
@Mapper(config = ProtoMapperConfig.class, uses = {TaskStateMapper.class, MessageMapper.class, CommonFieldMapper.class})
public interface TaskStatusMapper {

    TaskStatusMapper INSTANCE = Mappers.getMapper(TaskStatusMapper.class);

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
