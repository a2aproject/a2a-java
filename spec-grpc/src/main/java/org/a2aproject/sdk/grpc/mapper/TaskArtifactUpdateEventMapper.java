package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.TaskArtifactUpdateEvent} and {@link org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent}.
 * <p>
 * Now fully declarative using Builder pattern with @BeanMapping.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {ArtifactMapper.class, A2ACommonFieldMapper.class})
public interface TaskArtifactUpdateEventMapper {

    TaskArtifactUpdateEventMapper INSTANCE = A2AMappers.getMapper(TaskArtifactUpdateEventMapper.class);

    /**
     * Converts domain TaskArtifactUpdateEvent to proto.
     * Uses declarative mapping with CommonFieldMapper for metadata conversion.
     */
    @Mapping(target = "append", source = "append", conditionExpression = "java(domain.append() != null)")
    @Mapping(target = "lastChunk", source = "lastChunk", conditionExpression = "java(domain.lastChunk() != null)")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent toProto(TaskArtifactUpdateEvent domain);

    /**
     * Converts proto TaskArtifactUpdateEvent to domain.
     * Now fully declarative using Builder pattern configured via @BeanMapping.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    TaskArtifactUpdateEvent fromProto(org.a2aproject.sdk.grpc.TaskArtifactUpdateEvent proto);
}
