package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AgentCapabilities} and {@link org.a2aproject.sdk.grpc.AgentCapabilities}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {AgentExtensionMapper.class})
public interface AgentCapabilitiesMapper {

    AgentCapabilitiesMapper INSTANCE = A2AMappers.getMapper(AgentCapabilitiesMapper.class);

    org.a2aproject.sdk.grpc.AgentCapabilities toProto(org.a2aproject.sdk.spec.AgentCapabilities domain);

    org.a2aproject.sdk.spec.AgentCapabilities fromProto(org.a2aproject.sdk.grpc.AgentCapabilities proto);
}
