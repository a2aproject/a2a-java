package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AgentProvider} and {@link org.a2aproject.sdk.grpc.AgentProvider}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AgentProviderMapper {

    AgentProviderMapper INSTANCE = A2AMappers.getMapper(AgentProviderMapper.class);

    org.a2aproject.sdk.grpc.AgentProvider toProto(org.a2aproject.sdk.spec.AgentProvider domain);

    org.a2aproject.sdk.spec.AgentProvider fromProto(org.a2aproject.sdk.grpc.AgentProvider proto);
}
