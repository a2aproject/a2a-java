package io.a2a.grpc.mapper;

import io.a2a.spec.APIKeySecurityScheme;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.MutualTLSSecurityScheme;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.SecurityScheme} and {@link io.a2a.grpc.SecurityScheme}.
 * <p>
 * This mapper handles the polymorphic sealed interface SecurityScheme by using a custom
 * default method with switch expression. MapStruct doesn't natively support sealed interfaces
 * with protobuf's oneof pattern, so we manually dispatch to the appropriate concrete mapper.
 * <p>
 * <b>Manual Implementation Required:</b> Must use manual instanceof dispatch to handle sealed interface (5 permitted subtypes)
 * to protobuf oneof pattern, as MapStruct's @SubclassMapping cannot map different source types to different fields of the same target type.
 */
@Mapper(config = ProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {
            APIKeySecuritySchemeMapper.class,
            HTTPAuthSecuritySchemeMapper.class,
            OAuth2SecuritySchemeMapper.class,
            OpenIdConnectSecuritySchemeMapper.class,
            MutualTLSSecuritySchemeMapper.class
        })
public interface SecuritySchemeMapper {

    SecuritySchemeMapper INSTANCE = Mappers.getMapper(SecuritySchemeMapper.class);

    /**
     * Converts a domain SecurityScheme to protobuf SecurityScheme.
     * <p>
     * Uses instanceof checks to handle sealed interface polymorphism, dispatching to the
     * appropriate concrete mapper based on the runtime type. This is necessary because
     * MapStruct cannot automatically handle sealed interfaces with protobuf oneof fields.
     *
     * @param domain the domain security scheme (sealed interface)
     * @return the protobuf SecurityScheme with the appropriate oneof field set
     */
    default io.a2a.grpc.SecurityScheme toProto(io.a2a.spec.SecurityScheme domain) {
        if (domain == null) {
            return null;
        }

        if (domain instanceof APIKeySecurityScheme s) {
            return io.a2a.grpc.SecurityScheme.newBuilder()
                .setApiKeySecurityScheme(APIKeySecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof HTTPAuthSecurityScheme s) {
            return io.a2a.grpc.SecurityScheme.newBuilder()
                .setHttpAuthSecurityScheme(HTTPAuthSecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof OAuth2SecurityScheme s) {
            return io.a2a.grpc.SecurityScheme.newBuilder()
                .setOauth2SecurityScheme(OAuth2SecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof OpenIdConnectSecurityScheme s) {
            return io.a2a.grpc.SecurityScheme.newBuilder()
                .setOpenIdConnectSecurityScheme(OpenIdConnectSecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof MutualTLSSecurityScheme s) {
            return io.a2a.grpc.SecurityScheme.newBuilder()
                .setMtlsSecurityScheme(MutualTLSSecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        }

        throw new IllegalArgumentException("Unknown SecurityScheme type: " + domain.getClass());
    }
}
