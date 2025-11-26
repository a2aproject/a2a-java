package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.OAuthFlows} and {@link io.a2a.grpc.OAuthFlows}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {
            AuthorizationCodeOAuthFlowMapper.class,
            ClientCredentialsOAuthFlowMapper.class,
            ImplicitOAuthFlowMapper.class,
            PasswordOAuthFlowMapper.class
        })
public interface OAuthFlowsMapper {

    OAuthFlowsMapper INSTANCE = A2AMappers.getMapper(OAuthFlowsMapper.class);

    io.a2a.grpc.OAuthFlows toProto(io.a2a.spec.OAuthFlows domain);
}
