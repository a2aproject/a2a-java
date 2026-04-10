package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme} and {@link org.a2aproject.sdk.grpc.OpenIdConnectSecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface OpenIdConnectSecuritySchemeMapper {

    OpenIdConnectSecuritySchemeMapper INSTANCE = A2AMappers.getMapper(OpenIdConnectSecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    org.a2aproject.sdk.grpc.OpenIdConnectSecurityScheme toProto(org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme domain);

    default org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme fromProto(org.a2aproject.sdk.grpc.OpenIdConnectSecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme(proto.getOpenIdConnectUrl(), description);
    }
}
