package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.Artifact;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.Artifact} and {@link org.a2aproject.sdk.grpc.Artifact}.
 * <p>
 * Uses ADDER_PREFERRED strategy to use addAllExtensions() method instead of
 * trying to instantiate ProtocolStringList. Enables full compile-time validation!
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {PartMapper.class, A2ACommonFieldMapper.class})
public interface ArtifactMapper {

    ArtifactMapper INSTANCE = A2AMappers.getMapper(ArtifactMapper.class);

    /**
     * Converts domain Artifact to proto Artifact.
     * ADDER_PREFERRED strategy ensures MapStruct uses addAllExtensions() method.
     */
    @Mapping(target = "artifactId", source = "artifactId", conditionExpression = "java(domain.artifactId() != null)")
    @Mapping(target = "name", source = "name", conditionExpression = "java(domain.name() != null)")
    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    org.a2aproject.sdk.grpc.Artifact toProto(Artifact domain);

    /**
     * Converts proto Artifact to domain Artifact.
     * Handles empty string → null and Struct conversions via CommonFieldMapper.
     */
    @Mapping(target = "artifactId", source = "artifactId", qualifiedByName = "emptyToNull")
    @Mapping(target = "name", source = "name", qualifiedByName = "emptyToNull")
    @Mapping(target = "description", source = "description", qualifiedByName = "emptyToNull")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    @Mapping(target = "extensions", source = "extensions", qualifiedByName = "emptyListToNull")
    Artifact fromProto(org.a2aproject.sdk.grpc.Artifact proto);
}
