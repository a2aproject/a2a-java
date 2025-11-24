package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.AgentExtension} and {@link io.a2a.grpc.AgentExtension}.
 * <p>
 * Uses CommonFieldMapper for struct conversion (params field).
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {CommonFieldMapper.class})
public interface AgentExtensionMapper {

    AgentExtensionMapper INSTANCE = Mappers.getMapper(AgentExtensionMapper.class);

    /**
     * Converts domain AgentExtension to proto AgentExtension.
     * <p>
     * Maps params field via struct conversion.
     */
    @Mapping(target = "params", source = "params", conditionExpression = "java(domain.params() != null)", qualifiedByName = "mapToStruct")
    io.a2a.grpc.AgentExtension toProto(io.a2a.spec.AgentExtension domain);
}
