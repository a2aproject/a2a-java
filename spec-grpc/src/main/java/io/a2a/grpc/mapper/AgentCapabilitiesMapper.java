package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.AgentCapabilities} and {@link io.a2a.grpc.AgentCapabilities}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {AgentExtensionMapper.class})
public interface AgentCapabilitiesMapper {

    AgentCapabilitiesMapper INSTANCE = Mappers.getMapper(AgentCapabilitiesMapper.class);

    io.a2a.grpc.AgentCapabilities toProto(io.a2a.spec.AgentCapabilities domain);
}
