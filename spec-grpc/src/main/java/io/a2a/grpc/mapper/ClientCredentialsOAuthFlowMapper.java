package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.ClientCredentialsOAuthFlow} and {@link io.a2a.grpc.ClientCredentialsOAuthFlow}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ClientCredentialsOAuthFlowMapper {

    ClientCredentialsOAuthFlowMapper INSTANCE = Mappers.getMapper(ClientCredentialsOAuthFlowMapper.class);

    io.a2a.grpc.ClientCredentialsOAuthFlow toProto(io.a2a.spec.ClientCredentialsOAuthFlow domain);

    io.a2a.spec.ClientCredentialsOAuthFlow fromProto(io.a2a.grpc.ClientCredentialsOAuthFlow proto);
}
