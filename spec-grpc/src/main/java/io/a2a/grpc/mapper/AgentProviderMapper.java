package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.AgentProvider} and {@link io.a2a.grpc.AgentProvider}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AgentProviderMapper {

    AgentProviderMapper INSTANCE = A2AMappers.getMapper(AgentProviderMapper.class);

    io.a2a.grpc.AgentProvider toProto(io.a2a.spec.AgentProvider domain);

    io.a2a.spec.AgentProvider fromProto(io.a2a.grpc.AgentProvider proto);
}
