package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.OpenIdConnectSecurityScheme} and {@link io.a2a.grpc.OpenIdConnectSecurityScheme}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface OpenIdConnectSecuritySchemeMapper {

    OpenIdConnectSecuritySchemeMapper INSTANCE = Mappers.getMapper(OpenIdConnectSecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    io.a2a.grpc.OpenIdConnectSecurityScheme toProto(io.a2a.spec.OpenIdConnectSecurityScheme domain);
}
