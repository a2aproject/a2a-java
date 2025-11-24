package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.MutualTLSSecurityScheme} and {@link io.a2a.grpc.MutualTlsSecurityScheme}.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface MutualTLSSecuritySchemeMapper {

    MutualTLSSecuritySchemeMapper INSTANCE = Mappers.getMapper(MutualTLSSecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    io.a2a.grpc.MutualTlsSecurityScheme toProto(io.a2a.spec.MutualTLSSecurityScheme domain);
}
