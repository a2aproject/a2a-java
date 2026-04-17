package io.a2a.spec;

import static io.a2a.spec.APIKeySecurityScheme.API_KEY;

/**
 * Defines a security scheme that can be used to secure an agent's endpoints.
 * This is a discriminated union type based on the OpenAPI 3.0 Security Scheme Object.
 */
public sealed interface SecurityScheme permits APIKeySecurityScheme, HTTPAuthSecurityScheme, OAuth2SecurityScheme,
        OpenIdConnectSecurityScheme, MutualTLSSecurityScheme {

    String getDescription();
}
