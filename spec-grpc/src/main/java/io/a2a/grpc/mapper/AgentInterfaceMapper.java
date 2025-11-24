package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.AgentInterface} and {@link io.a2a.grpc.AgentInterface}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AgentInterfaceMapper {

    AgentInterfaceMapper INSTANCE = Mappers.getMapper(AgentInterfaceMapper.class);

    io.a2a.grpc.AgentInterface toProto(io.a2a.spec.AgentInterface domain);

    io.a2a.spec.AgentInterface fromProto(io.a2a.grpc.AgentInterface proto);
}
