package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.APIKeySecurityScheme;
import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;
import org.a2aproject.sdk.spec.MutualTLSSecurityScheme;
import org.a2aproject.sdk.spec.OAuth2SecurityScheme;
import org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.SecurityScheme} and {@link org.a2aproject.sdk.grpc.SecurityScheme}.
 * <p>
 * This mapper handles the polymorphic sealed interface SecurityScheme by using a custom
 * default method with switch expression. MapStruct doesn't natively support sealed interfaces
 * with protobuf's oneof pattern, so we manually dispatch to the appropriate concrete mapper.
 * <p>
 * <b>Manual Implementation Required:</b> Must use manual instanceof dispatch to handle sealed interface (5 permitted subtypes)
 * to protobuf oneof pattern, as MapStruct's @SubclassMapping cannot map different source types to different fields of the same target type.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {
            APIKeySecuritySchemeMapper.class,
            HTTPAuthSecuritySchemeMapper.class,
            OAuth2SecuritySchemeMapper.class,
            OpenIdConnectSecuritySchemeMapper.class,
            MutualTLSSecuritySchemeMapper.class
        })
public interface SecuritySchemeMapper {

    SecuritySchemeMapper INSTANCE = A2AMappers.getMapper(SecuritySchemeMapper.class);

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
    default org.a2aproject.sdk.grpc.SecurityScheme toProto(org.a2aproject.sdk.spec.SecurityScheme domain) {
        if (domain == null) {
            return null;
        }

        if (domain instanceof APIKeySecurityScheme s) {
            return org.a2aproject.sdk.grpc.SecurityScheme.newBuilder()
                .setApiKeySecurityScheme(APIKeySecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof HTTPAuthSecurityScheme s) {
            return org.a2aproject.sdk.grpc.SecurityScheme.newBuilder()
                .setHttpAuthSecurityScheme(HTTPAuthSecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof OAuth2SecurityScheme s) {
            return org.a2aproject.sdk.grpc.SecurityScheme.newBuilder()
                .setOauth2SecurityScheme(OAuth2SecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof OpenIdConnectSecurityScheme s) {
            return org.a2aproject.sdk.grpc.SecurityScheme.newBuilder()
                .setOpenIdConnectSecurityScheme(OpenIdConnectSecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        } else if (domain instanceof MutualTLSSecurityScheme s) {
            return org.a2aproject.sdk.grpc.SecurityScheme.newBuilder()
                .setMtlsSecurityScheme(MutualTLSSecuritySchemeMapper.INSTANCE.toProto(s))
                .build();
        }

        throw new IllegalArgumentException("Unknown SecurityScheme type: " + domain.getClass());
    }

    /**
     * Converts a protobuf SecurityScheme to domain SecurityScheme.
     * <p>
     * Uses oneof checks to determine which security scheme type is set, dispatching to the
     * appropriate concrete mapper. This reverses the toProto conversion by checking which
     * oneof field is populated.
     *
     * @param proto the protobuf SecurityScheme with a oneof field set
     * @return the domain security scheme (sealed interface implementation)
     */
    default org.a2aproject.sdk.spec.SecurityScheme fromProto(org.a2aproject.sdk.grpc.SecurityScheme proto) {
        if (proto == null) {
            return null;
        }

        return switch (proto.getSchemeCase()) {
            case API_KEY_SECURITY_SCHEME ->
                APIKeySecuritySchemeMapper.INSTANCE.fromProto(proto.getApiKeySecurityScheme());
            case HTTP_AUTH_SECURITY_SCHEME ->
                HTTPAuthSecuritySchemeMapper.INSTANCE.fromProto(proto.getHttpAuthSecurityScheme());
            case OAUTH2_SECURITY_SCHEME ->
                OAuth2SecuritySchemeMapper.INSTANCE.fromProto(proto.getOauth2SecurityScheme());
            case OPEN_ID_CONNECT_SECURITY_SCHEME ->
                OpenIdConnectSecuritySchemeMapper.INSTANCE.fromProto(proto.getOpenIdConnectSecurityScheme());
            case MTLS_SECURITY_SCHEME ->
                MutualTLSSecuritySchemeMapper.INSTANCE.fromProto(proto.getMtlsSecurityScheme());
            case SCHEME_NOT_SET ->
                throw new IllegalArgumentException("SecurityScheme oneof field not set");
        };
    }
}
