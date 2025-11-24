package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.PasswordOAuthFlow} and {@link io.a2a.grpc.PasswordOAuthFlow}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface PasswordOAuthFlowMapper {

    PasswordOAuthFlowMapper INSTANCE = Mappers.getMapper(PasswordOAuthFlowMapper.class);

    io.a2a.grpc.PasswordOAuthFlow toProto(io.a2a.spec.PasswordOAuthFlow domain);

    io.a2a.spec.PasswordOAuthFlow fromProto(io.a2a.grpc.PasswordOAuthFlow proto);
}
