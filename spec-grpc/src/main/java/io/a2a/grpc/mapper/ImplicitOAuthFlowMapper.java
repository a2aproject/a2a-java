package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.ImplicitOAuthFlow} and {@link io.a2a.grpc.ImplicitOAuthFlow}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ImplicitOAuthFlowMapper {

    ImplicitOAuthFlowMapper INSTANCE = A2AMappers.getMapper(ImplicitOAuthFlowMapper.class);

    io.a2a.grpc.ImplicitOAuthFlow toProto(io.a2a.spec.ImplicitOAuthFlow domain);

    io.a2a.spec.ImplicitOAuthFlow fromProto(io.a2a.grpc.ImplicitOAuthFlow proto);
}
