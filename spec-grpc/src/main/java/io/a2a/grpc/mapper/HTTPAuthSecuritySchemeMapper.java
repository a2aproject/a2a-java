package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.HTTPAuthSecurityScheme} and {@link io.a2a.grpc.HTTPAuthSecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface HTTPAuthSecuritySchemeMapper {

    HTTPAuthSecuritySchemeMapper INSTANCE = A2AMappers.getMapper(HTTPAuthSecuritySchemeMapper.class);

    @Mapping(target = "bearerFormat", source = "bearerFormat", conditionExpression = "java(domain.bearerFormat() != null)")
    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    io.a2a.grpc.HTTPAuthSecurityScheme toProto(io.a2a.spec.HTTPAuthSecurityScheme domain);

    default io.a2a.spec.HTTPAuthSecurityScheme fromProto(io.a2a.grpc.HTTPAuthSecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        String bearerFormat = proto.getBearerFormat().isEmpty() ? null : proto.getBearerFormat();
        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new io.a2a.spec.HTTPAuthSecurityScheme(bearerFormat, proto.getScheme(), description);
    }
}
