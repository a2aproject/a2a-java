package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.AuthorizationCodeOAuthFlow} and {@link io.a2a.grpc.AuthorizationCodeOAuthFlow}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AuthorizationCodeOAuthFlowMapper {

    AuthorizationCodeOAuthFlowMapper INSTANCE = Mappers.getMapper(AuthorizationCodeOAuthFlowMapper.class);

    io.a2a.grpc.AuthorizationCodeOAuthFlow toProto(io.a2a.spec.AuthorizationCodeOAuthFlow domain);

    io.a2a.spec.AuthorizationCodeOAuthFlow fromProto(io.a2a.grpc.AuthorizationCodeOAuthFlow proto);
}
