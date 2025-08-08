package io.a2a.spec;

import static io.a2a.spec.APIKeySecurityScheme.API_KEY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed interface representing security schemes in the A2A protocol.
 * <p>
 * This interface defines the contract for various authentication and authorization
 * mechanisms that can be used to secure A2A agent communications. The interface
 * uses Jackson annotations to support polymorphic JSON serialization and
 * deserialization based on the "type" property.
 * </p>
 * <p>
 * Supported security scheme types:
 * </p>
 * <ul>
 * <li>{@link APIKeySecurityScheme} - API key-based authentication</li>
 * <li>{@link HTTPAuthSecurityScheme} - HTTP authentication (Basic, Bearer, etc.)</li>
 * <li>{@link OAuth2SecurityScheme} - OAuth 2.0 authentication</li>
 * <li>{@link OpenIdConnectSecurityScheme} - OpenID Connect authentication</li>
 * </ul>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = APIKeySecurityScheme.class, name = API_KEY),
        @JsonSubTypes.Type(value = HTTPAuthSecurityScheme.class, name = HTTPAuthSecurityScheme.HTTP),
        @JsonSubTypes.Type(value = OAuth2SecurityScheme.class, name = OAuth2SecurityScheme.OAUTH2),
        @JsonSubTypes.Type(value = OpenIdConnectSecurityScheme.class, name = OpenIdConnectSecurityScheme.OPENID_CONNECT)
})
public sealed interface SecurityScheme permits APIKeySecurityScheme, HTTPAuthSecurityScheme, OAuth2SecurityScheme, OpenIdConnectSecurityScheme {

    /**
     * Gets the description of this security scheme.
     *
     * @return a human-readable description of the security scheme
     */
    String getDescription();
}
