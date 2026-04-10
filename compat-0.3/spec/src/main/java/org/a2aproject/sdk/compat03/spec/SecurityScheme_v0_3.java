package org.a2aproject.sdk.compat03.spec;

/**
 * Defines a security scheme that can be used to secure an agent's endpoints.
 * This is a discriminated union type based on the OpenAPI 3.0 Security Scheme Object.
 */
public sealed interface SecurityScheme_v0_3 permits APIKeySecurityScheme_v0_3, HTTPAuthSecurityScheme_v0_3, OAuth2SecurityScheme_v0_3,
        OpenIdConnectSecurityScheme_v0_3, MutualTLSSecurityScheme_v0_3 {

    String getDescription();
}
