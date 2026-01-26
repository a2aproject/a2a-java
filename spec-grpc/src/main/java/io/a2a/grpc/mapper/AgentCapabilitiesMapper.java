package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.AgentCapabilities} and {@link io.a2a.grpc.AgentCapabilities}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {AgentExtensionMapper.class})
public interface AgentCapabilitiesMapper {

    AgentCapabilitiesMapper INSTANCE = A2AMappers.getMapper(AgentCapabilitiesMapper.class);

    io.a2a.grpc.AgentCapabilities toProto(io.a2a.spec.AgentCapabilities domain);

    io.a2a.spec.AgentCapabilities fromProto(io.a2a.grpc.AgentCapabilities proto);
}
