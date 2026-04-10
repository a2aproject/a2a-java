package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AgentExtension} and {@link org.a2aproject.sdk.grpc.AgentExtension}.
 * <p>
 * Uses CommonFieldMapper for struct conversion (params field).
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {A2ACommonFieldMapper.class})
public interface AgentExtensionMapper {

    AgentExtensionMapper INSTANCE = A2AMappers.getMapper(AgentExtensionMapper.class);

    /**
     * Converts domain AgentExtension to proto AgentExtension.
     * <p>
     * Maps params field via struct conversion.
     */
    @Mapping(target = "params", source = "params", conditionExpression = "java(domain.params() != null)", qualifiedByName = "mapToStruct")
    org.a2aproject.sdk.grpc.AgentExtension toProto(org.a2aproject.sdk.spec.AgentExtension domain);

    /**
     * Converts proto AgentExtension to domain AgentExtension.
     * <p>
     * Maps params field from struct to map.
     */
    @Mapping(target = "params", source = "params", qualifiedByName = "structToMap")
    org.a2aproject.sdk.spec.AgentExtension fromProto(org.a2aproject.sdk.grpc.AgentExtension proto);
}
