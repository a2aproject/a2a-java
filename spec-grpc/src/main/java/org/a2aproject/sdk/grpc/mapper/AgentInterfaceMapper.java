package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AgentInterface} and {@link org.a2aproject.sdk.grpc.AgentInterface}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AgentInterfaceMapper {

    AgentInterfaceMapper INSTANCE = A2AMappers.getMapper(AgentInterfaceMapper.class);

    org.a2aproject.sdk.grpc.AgentInterface toProto(org.a2aproject.sdk.spec.AgentInterface domain);

    org.a2aproject.sdk.spec.AgentInterface fromProto(org.a2aproject.sdk.grpc.AgentInterface proto);
}
