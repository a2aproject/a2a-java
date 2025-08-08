package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an OpenID Connect security scheme configuration for the A2A protocol.
 * This record defines the OpenID Connect (OIDC) authentication mechanism that allows
 * agents to authenticate users through an identity provider.
 *
 * <p>OpenID Connect is an identity layer built on top of OAuth 2.0 that enables
 * clients to verify the identity of end-users based on authentication performed
 * by an authorization server. It provides a standardized way to obtain basic
 * profile information about the user.</p>
 *
 * <p>Key components of this security scheme:</p>
 * <ul>
 *   <li><strong>Type:</strong> Always "openIdConnect" to identify this as an OIDC scheme</li>
 *   <li><strong>Description:</strong> Human-readable description of the authentication mechanism</li>
 *   <li><strong>OpenID Connect URL:</strong> The discovery endpoint for the OIDC provider</li>
 * </ul>
 *
 * <p>This implementation follows the OpenAPI Security Scheme Object specification
 * for OpenID Connect, ensuring compatibility with standard OIDC implementations.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OpenIdConnectSecurityScheme implements SecurityScheme {

    public static final String OPENID_CONNECT = "openIdConnect";
    private final String openIdConnectUrl;
    private final String description;
    private final String type;

    public OpenIdConnectSecurityScheme(String openIdConnectUrl, String description) {
        this(openIdConnectUrl, description, OPENID_CONNECT);
    }

    @JsonCreator
    public OpenIdConnectSecurityScheme(@JsonProperty("openIdConnectUrl") String openIdConnectUrl,
                                       @JsonProperty("description") String description, @JsonProperty("type") String type) {
        if (!type.equals(OPENID_CONNECT)) {
            throw new IllegalArgumentException("Invalid type for OpenIdConnectSecurityScheme");
        }
        this.openIdConnectUrl = openIdConnectUrl;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    public String getType() {
        return type;
    }

    /**
     * Builder class for constructing OpenIdConnectSecurityScheme instances.
     * Provides a fluent API for configuring OpenID Connect security schemes.
     */
    public static class Builder {
        /** The OpenID Connect discovery URL */
        private String openIdConnectUrl;
        
        /** Description of the authentication mechanism */
        private String description;

        /**
         * Sets the OpenID Connect discovery URL.
         * This URL should point to the well-known configuration endpoint
         * of the OpenID Connect provider (typically ending with /.well-known/openid_configuration).
         * 
         * @param openIdConnectUrl the discovery URL for the OIDC provider
         * @return this builder instance for method chaining
         */
        public Builder openIdConnectUrl(String openIdConnectUrl) {
            this.openIdConnectUrl = openIdConnectUrl;
            return this;
        }

        /**
         * Sets the human-readable description of the OpenID Connect authentication.
         * 
         * @param description a description explaining how the authentication works
         * @return this builder instance for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new OpenIdConnectSecurityScheme instance.
         * 
         * @return a new OpenIdConnectSecurityScheme with the configured settings
         */
        public OpenIdConnectSecurityScheme build() {
            return new OpenIdConnectSecurityScheme(openIdConnectUrl, description);
        }
    }

}
