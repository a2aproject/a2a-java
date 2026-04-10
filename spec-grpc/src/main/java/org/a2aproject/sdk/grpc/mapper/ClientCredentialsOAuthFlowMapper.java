package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow} and {@link org.a2aproject.sdk.grpc.ClientCredentialsOAuthFlow}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ClientCredentialsOAuthFlowMapper {

    ClientCredentialsOAuthFlowMapper INSTANCE = A2AMappers.getMapper(ClientCredentialsOAuthFlowMapper.class);

    org.a2aproject.sdk.grpc.ClientCredentialsOAuthFlow toProto(org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow domain);

    org.a2aproject.sdk.spec.ClientCredentialsOAuthFlow fromProto(org.a2aproject.sdk.grpc.ClientCredentialsOAuthFlow proto);
}
