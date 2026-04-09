package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.AuthorizationCodeOAuthFlow} and {@link org.a2aproject.sdk.grpc.AuthorizationCodeOAuthFlow}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface AuthorizationCodeOAuthFlowMapper {

    AuthorizationCodeOAuthFlowMapper INSTANCE = A2AMappers.getMapper(AuthorizationCodeOAuthFlowMapper.class);

    org.a2aproject.sdk.grpc.AuthorizationCodeOAuthFlow toProto(org.a2aproject.sdk.spec.AuthorizationCodeOAuthFlow domain);

    org.a2aproject.sdk.spec.AuthorizationCodeOAuthFlow fromProto(org.a2aproject.sdk.grpc.AuthorizationCodeOAuthFlow proto);
}
