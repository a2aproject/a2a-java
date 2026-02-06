package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.AgentInterface} and {@link io.a2a.grpc.AgentInterface}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AgentInterfaceMapper {

    AgentInterfaceMapper INSTANCE = A2AMappers.getMapper(AgentInterfaceMapper.class);

    io.a2a.grpc.AgentInterface toProto(io.a2a.spec.AgentInterface domain);

    io.a2a.spec.AgentInterface fromProto(io.a2a.grpc.AgentInterface proto);
}
