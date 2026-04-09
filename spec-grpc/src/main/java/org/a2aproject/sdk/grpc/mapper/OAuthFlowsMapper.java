package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.OAuthFlows} and {@link org.a2aproject.sdk.grpc.OAuthFlows}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {
            AuthorizationCodeOAuthFlowMapper.class,
            ClientCredentialsOAuthFlowMapper.class
        })
public interface OAuthFlowsMapper {

    OAuthFlowsMapper INSTANCE = A2AMappers.getMapper(OAuthFlowsMapper.class);

    @Mapping(target = "implicit", ignore = true)
    @Mapping(target = "password", ignore = true)
    org.a2aproject.sdk.grpc.OAuthFlows toProto(org.a2aproject.sdk.spec.OAuthFlows domain);

    org.a2aproject.sdk.spec.OAuthFlows fromProto(org.a2aproject.sdk.grpc.OAuthFlows proto);
}
