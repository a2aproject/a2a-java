package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.HTTPAuthSecurityScheme} and {@link io.a2a.grpc.HTTPAuthSecurityScheme}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface HTTPAuthSecuritySchemeMapper {

    HTTPAuthSecuritySchemeMapper INSTANCE = Mappers.getMapper(HTTPAuthSecuritySchemeMapper.class);

    @Mapping(target = "bearerFormat", source = "bearerFormat", conditionExpression = "java(domain.getBearerFormat() != null)")
    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    io.a2a.grpc.HTTPAuthSecurityScheme toProto(io.a2a.spec.HTTPAuthSecurityScheme domain);
}
