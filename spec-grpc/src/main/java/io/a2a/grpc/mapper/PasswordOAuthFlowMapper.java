package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.PasswordOAuthFlow} and {@link io.a2a.grpc.PasswordOAuthFlow}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PasswordOAuthFlowMapper {

    PasswordOAuthFlowMapper INSTANCE = A2AMappers.getMapper(PasswordOAuthFlowMapper.class);

    io.a2a.grpc.PasswordOAuthFlow toProto(io.a2a.spec.PasswordOAuthFlow domain);

    io.a2a.spec.PasswordOAuthFlow fromProto(io.a2a.grpc.PasswordOAuthFlow proto);
}
