package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.AgentProvider} and {@link io.a2a.grpc.AgentProvider}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AgentProviderMapper {

    AgentProviderMapper INSTANCE = Mappers.getMapper(AgentProviderMapper.class);

    io.a2a.grpc.AgentProvider toProto(io.a2a.spec.AgentProvider domain);

    io.a2a.spec.AgentProvider fromProto(io.a2a.grpc.AgentProvider proto);
}
