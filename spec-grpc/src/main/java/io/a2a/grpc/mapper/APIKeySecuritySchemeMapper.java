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
}
