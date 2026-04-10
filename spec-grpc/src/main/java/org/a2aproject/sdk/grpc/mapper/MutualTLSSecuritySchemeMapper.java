package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.MutualTLSSecurityScheme} and {@link org.a2aproject.sdk.grpc.MutualTlsSecurityScheme}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface MutualTLSSecuritySchemeMapper {

    MutualTLSSecuritySchemeMapper INSTANCE = A2AMappers.getMapper(MutualTLSSecuritySchemeMapper.class);

    @Mapping(target = "description", source = "description", conditionExpression = "java(domain.description() != null)")
    org.a2aproject.sdk.grpc.MutualTlsSecurityScheme toProto(org.a2aproject.sdk.spec.MutualTLSSecurityScheme domain);

    default org.a2aproject.sdk.spec.MutualTLSSecurityScheme fromProto(org.a2aproject.sdk.grpc.MutualTlsSecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        String description = proto.getDescription().isEmpty() ? null : proto.getDescription();

        return new org.a2aproject.sdk.spec.MutualTLSSecurityScheme(description);
    }
}
