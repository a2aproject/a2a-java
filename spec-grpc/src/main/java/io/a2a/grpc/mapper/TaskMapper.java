package io.a2a.grpc.mapper;

import io.a2a.spec.Task;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.Task} and {@link io.a2a.grpc.Task}.
 * <p>
 * Uses ADDER_PREFERRED strategy for List fields (artifacts, history)
 * to use addAllArtifacts() and addAllHistory() methods.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {TaskStatusMapper.class, ArtifactMapper.class, MessageMapper.class, A2ACommonFieldMapper.class})
public interface TaskMapper {

    TaskMapper INSTANCE = A2AMappers.getMapper(TaskMapper.class);

    /**
     * Converts domain Task to proto Task.
     * Uses CommonFieldMapper for metadata conversion and ADDER_PREFERRED for lists.
     */
    @Mapping(target = "id", source = "id", conditionExpression = "java(domain.id() != null)")
    @Mapping(target = "contextId", source = "contextId", conditionExpression = "java(domain.contextId() != null)")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    io.a2a.grpc.Task toProto(Task domain);

    /**
     * Converts proto Task to domain Task.
     * Handles empty string → null and Struct conversions via CommonFieldMapper.
     * Uses Builder pattern explicitly configured via @BeanMapping.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "id", qualifiedByName = "emptyToNull")
    @Mapping(target = "contextId", source = "contextId", qualifiedByName = "emptyToNull")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    Task fromProto(io.a2a.grpc.Task proto);
}
