package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.OAuthFlows} and {@link io.a2a.grpc.OAuthFlows}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {
            AuthorizationCodeOAuthFlowMapper.class,
            ClientCredentialsOAuthFlowMapper.class,
            ImplicitOAuthFlowMapper.class,
            PasswordOAuthFlowMapper.class
        })
public interface OAuthFlowsMapper {

    OAuthFlowsMapper INSTANCE = Mappers.getMapper(OAuthFlowsMapper.class);

    io.a2a.grpc.OAuthFlows toProto(io.a2a.spec.OAuthFlows domain);
}
