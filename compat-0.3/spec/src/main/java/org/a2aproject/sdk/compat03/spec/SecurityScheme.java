package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme.API_KEY;

/**
 * Defines a security scheme that can be used to secure an agent's endpoints.
 * This is a discriminated union type based on the OpenAPI 3.0 Security Scheme Object.
 */
public sealed interface SecurityScheme permits APIKeySecurityScheme, HTTPAuthSecurityScheme, OAuth2SecurityScheme,
        OpenIdConnectSecurityScheme, MutualTLSSecurityScheme {

    String getDescription();
}
