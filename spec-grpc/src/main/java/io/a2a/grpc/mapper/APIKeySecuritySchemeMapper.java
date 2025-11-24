package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.APIKeySecurityScheme} and {@link io.a2a.grpc.APIKeySecurityScheme}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface APIKeySecuritySchemeMapper {

    APIKeySecuritySchemeMapper INSTANCE = Mappers.getMapper(APIKeySecuritySchemeMapper.class);

    // location enum is converted to string via ProtoMapperConfig.map(Location)
    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    io.a2a.grpc.APIKeySecurityScheme toProto(io.a2a.spec.APIKeySecurityScheme domain);
}
