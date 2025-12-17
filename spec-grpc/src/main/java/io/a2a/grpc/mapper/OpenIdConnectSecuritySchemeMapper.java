package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.OpenIdConnectSecurityScheme} and {@link io.a2a.grpc.OpenIdConnectSecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface OpenIdConnectSecuritySchemeMapper {

    OpenIdConnectSecuritySchemeMapper INSTANCE = A2AMappers.getMapper(OpenIdConnectSecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    io.a2a.grpc.OpenIdConnectSecurityScheme toProto(io.a2a.spec.OpenIdConnectSecurityScheme domain);

    default io.a2a.spec.OpenIdConnectSecurityScheme fromProto(io.a2a.grpc.OpenIdConnectSecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new io.a2a.spec.OpenIdConnectSecurityScheme(proto.getOpenIdConnectUrl(), description);
    }
}
