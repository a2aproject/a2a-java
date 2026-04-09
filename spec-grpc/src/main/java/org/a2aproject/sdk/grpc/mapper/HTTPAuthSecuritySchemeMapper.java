package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.HTTPAuthSecurityScheme} and {@link org.a2aproject.sdk.grpc.HTTPAuthSecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface HTTPAuthSecuritySchemeMapper {

    HTTPAuthSecuritySchemeMapper INSTANCE = A2AMappers.getMapper(HTTPAuthSecuritySchemeMapper.class);

    @Mapping(target = "bearerFormat", source = "bearerFormat", conditionExpression = "java(domain.bearerFormat() != null)")
    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    org.a2aproject.sdk.grpc.HTTPAuthSecurityScheme toProto(org.a2aproject.sdk.spec.HTTPAuthSecurityScheme domain);

    default org.a2aproject.sdk.spec.HTTPAuthSecurityScheme fromProto(org.a2aproject.sdk.grpc.HTTPAuthSecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        String bearerFormat = proto.getBearerFormat().isEmpty() ? null : proto.getBearerFormat();
        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new org.a2aproject.sdk.spec.HTTPAuthSecurityScheme(bearerFormat, proto.getScheme(), description);
    }
}
