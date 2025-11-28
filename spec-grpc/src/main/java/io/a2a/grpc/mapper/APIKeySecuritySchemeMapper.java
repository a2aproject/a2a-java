package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.APIKeySecurityScheme} and {@link io.a2a.grpc.APIKeySecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface APIKeySecuritySchemeMapper {

    APIKeySecuritySchemeMapper INSTANCE = A2AMappers.getMapper(APIKeySecuritySchemeMapper.class);

    // location enum is converted to string via ProtoMapperConfig.map(Location)
    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    io.a2a.grpc.APIKeySecurityScheme toProto(io.a2a.spec.APIKeySecurityScheme domain);

    default io.a2a.spec.APIKeySecurityScheme fromProto(io.a2a.grpc.APIKeySecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        io.a2a.spec.APIKeySecurityScheme.Location location =
            io.a2a.spec.APIKeySecurityScheme.Location.fromString(proto.getLocation());
        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new io.a2a.spec.APIKeySecurityScheme(location, proto.getName(), description);
    }
}
