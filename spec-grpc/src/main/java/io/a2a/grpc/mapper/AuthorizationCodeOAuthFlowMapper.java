package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.AuthorizationCodeOAuthFlow} and {@link io.a2a.grpc.AuthorizationCodeOAuthFlow}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AuthorizationCodeOAuthFlowMapper {

    AuthorizationCodeOAuthFlowMapper INSTANCE = A2AMappers.getMapper(AuthorizationCodeOAuthFlowMapper.class);

    io.a2a.grpc.AuthorizationCodeOAuthFlow toProto(io.a2a.spec.AuthorizationCodeOAuthFlow domain);

    io.a2a.spec.AuthorizationCodeOAuthFlow fromProto(io.a2a.grpc.AuthorizationCodeOAuthFlow proto);
}
