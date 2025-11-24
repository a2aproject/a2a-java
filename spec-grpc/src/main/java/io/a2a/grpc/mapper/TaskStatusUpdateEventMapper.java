package io.a2a.grpc.mapper;

import io.a2a.spec.TaskStatusUpdateEvent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.TaskStatusUpdateEvent} and {@link io.a2a.grpc.TaskStatusUpdateEvent}.
 * <p>
 * Now fully declarative using Builder pattern with @BeanMapping.
 * Builder's isFinal() method handles the Java "final" keyword mapping.
 */
@Mapper(config = ProtoMapperConfig.class, uses = {TaskStatusMapper.class, CommonFieldMapper.class})
public interface TaskStatusUpdateEventMapper {

    TaskStatusUpdateEventMapper INSTANCE = Mappers.getMapper(TaskStatusUpdateEventMapper.class);

    /**
     * Converts domain TaskStatusUpdateEvent to proto.
     * Uses declarative mapping with CommonFieldMapper for metadata conversion.
     */
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    @Mapping(target = "final", source = "final")
    io.a2a.grpc.TaskStatusUpdateEvent toProto(TaskStatusUpdateEvent domain);

    /**
     * Converts proto TaskStatusUpdateEvent to domain.
     * Now fully declarative using Builder pattern configured via @BeanMapping.
     * MapStruct automatically maps proto.getFinal() → builder.isFinal().
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "isFinal", source = "final")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    TaskStatusUpdateEvent fromProto(io.a2a.grpc.TaskStatusUpdateEvent proto);
}
