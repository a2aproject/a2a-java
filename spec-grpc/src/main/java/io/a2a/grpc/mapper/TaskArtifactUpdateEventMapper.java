package io.a2a.grpc.mapper;

import io.a2a.spec.TaskArtifactUpdateEvent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.TaskArtifactUpdateEvent} and {@link io.a2a.grpc.TaskArtifactUpdateEvent}.
 * <p>
 * Now fully declarative using Builder pattern with @BeanMapping.
 */
@Mapper(config = ProtoMapperConfig.class, uses = {ArtifactMapper.class, CommonFieldMapper.class})
public interface TaskArtifactUpdateEventMapper {

    TaskArtifactUpdateEventMapper INSTANCE = Mappers.getMapper(TaskArtifactUpdateEventMapper.class);

    /**
     * Converts domain TaskArtifactUpdateEvent to proto.
     * Uses declarative mapping with CommonFieldMapper for metadata conversion.
     */
    @Mapping(target = "append", source = "append", conditionExpression = "java(domain.isAppend() != null)")
    @Mapping(target = "lastChunk", source = "lastChunk", conditionExpression = "java(domain.isLastChunk() != null)")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    io.a2a.grpc.TaskArtifactUpdateEvent toProto(TaskArtifactUpdateEvent domain);

    /**
     * Converts proto TaskArtifactUpdateEvent to domain.
     * Now fully declarative using Builder pattern configured via @BeanMapping.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    TaskArtifactUpdateEvent fromProto(io.a2a.grpc.TaskArtifactUpdateEvent proto);
}
