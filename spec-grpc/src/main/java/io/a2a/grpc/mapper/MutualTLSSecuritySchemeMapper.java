package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.MutualTLSSecurityScheme} and {@link io.a2a.grpc.MutualTlsSecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface MutualTLSSecuritySchemeMapper {

    MutualTLSSecuritySchemeMapper INSTANCE = A2AMappers.getMapper(MutualTLSSecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.getDescription() != null)")
    io.a2a.grpc.MutualTlsSecurityScheme toProto(io.a2a.spec.MutualTLSSecurityScheme domain);

    default io.a2a.spec.MutualTLSSecurityScheme fromProto(io.a2a.grpc.MutualTlsSecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new io.a2a.spec.MutualTLSSecurityScheme(description);
    }
}
