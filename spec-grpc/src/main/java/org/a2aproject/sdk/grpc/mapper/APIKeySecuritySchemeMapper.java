package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.APIKeySecurityScheme} and {@link org.a2aproject.sdk.grpc.APIKeySecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface APIKeySecuritySchemeMapper {

    APIKeySecuritySchemeMapper INSTANCE = A2AMappers.getMapper(APIKeySecuritySchemeMapper.class);

    // location enum is converted to string via ProtoMapperConfig.map(Location)
    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    org.a2aproject.sdk.grpc.APIKeySecurityScheme toProto(org.a2aproject.sdk.spec.APIKeySecurityScheme domain);

    default org.a2aproject.sdk.spec.APIKeySecurityScheme fromProto(org.a2aproject.sdk.grpc.APIKeySecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        org.a2aproject.sdk.spec.APIKeySecurityScheme.Location location =
            org.a2aproject.sdk.spec.APIKeySecurityScheme.Location.fromString(proto.getLocation());
        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new org.a2aproject.sdk.spec.APIKeySecurityScheme(location, proto.getName(), description);
    }
}
