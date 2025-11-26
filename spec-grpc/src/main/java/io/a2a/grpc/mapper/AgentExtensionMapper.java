package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.AgentExtension} and {@link io.a2a.grpc.AgentExtension}.
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
    io.a2a.grpc.AgentExtension toProto(io.a2a.spec.AgentExtension domain);
}
