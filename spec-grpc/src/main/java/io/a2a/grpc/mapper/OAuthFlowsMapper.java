package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.OAuthFlows} and {@link io.a2a.grpc.OAuthFlows}.
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
    io.a2a.grpc.OAuthFlows toProto(io.a2a.spec.OAuthFlows domain);

    io.a2a.spec.OAuthFlows fromProto(io.a2a.grpc.OAuthFlows proto);
}
