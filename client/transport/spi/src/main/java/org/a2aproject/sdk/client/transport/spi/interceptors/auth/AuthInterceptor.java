package org.a2aproject.sdk.client.transport.spi.interceptors.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.spec.APIKeySecurityScheme;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;
import org.a2aproject.sdk.spec.OAuth2SecurityScheme;
import org.a2aproject.sdk.spec.OpenIdConnectSecurityScheme;
import org.a2aproject.sdk.spec.SecurityRequirement;
import org.a2aproject.sdk.spec.SecurityScheme;
import org.jspecify.annotations.Nullable;

/**
 * An interceptor that automatically adds authentication details to requests
 * based on the agent's security schemes and the credentials available.
 */
public class AuthInterceptor extends ClientCallInterceptor {

    private static final String BEARER_SCHEME = "bearer";
    private static final String BASIC_SCHEME = "basic";
    public static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String BASIC = "Basic ";

    /**
     * Allowlist of header names that are safe for API key injection.
     * This prevents credential leakage through malicious header names that could
     * be forwarded to third-party origins during redirects or other scenarios.
     * Only standard authentication-related headers are permitted.
     * Header names are stored in lowercase for case-insensitive comparison.
     */
    private static final Set<String> SAFE_API_KEY_HEADER_NAMES = Set.of(
        "authorization",
        "x-api-key",
        "api-key",
        "x-auth-token",
        "x-authentication"
    );

    private final CredentialService credentialService;

    public AuthInterceptor(final CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    public PayloadAndHeaders intercept(String methodName, Object payload, Map<String, String> headers,
                                       @Nullable AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        Map<String, String> updatedHeaders = new HashMap<>(headers == null ? new HashMap<>() : headers);
        if (agentCard != null) {
            Map<String, String> credentials = resolveCredentials(agentCard, clientCallContext);
            if (credentials != null) {
                credentials.forEach((name, value) -> {
                    updatedHeaders.keySet().removeIf(existing -> existing.equalsIgnoreCase(name));
                    updatedHeaders.put(name, value);
                });
            }
        }
        return new PayloadAndHeaders(payload, updatedHeaders);
    }

    private @Nullable Map<String, String> resolveCredentials(
            AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        if (agentCard.securityRequirements() == null || agentCard.securityRequirements().isEmpty()) {
            return Map.of();
        }
        var schemes = agentCard.securitySchemes();
        for (SecurityRequirement requirement : agentCard.securityRequirements()) {
            if (requirement == null) {
                continue;
            }
            Map<String, String> headers = resolveAlternative(requirement.schemes().keySet(), schemes,
                    clientCallContext);
            if (headers != null) {
                return headers;
            }
        }
        return null;
    }

    private @Nullable Map<String, String> resolveAlternative(Set<String> names,
            @Nullable Map<String, SecurityScheme> schemes,
            @Nullable ClientCallContext context) {
        Map<String, String> headers = new HashMap<>();
        Set<String> headerNames = new HashSet<>();
        for (String name : names) {
            SecurityScheme scheme = schemes == null ? null : schemes.get(name);
            HeaderCredential header = headerForScheme(scheme);
            if (header == null || !headerNames.add(header.name().toLowerCase(Locale.ROOT))) {
                return null;
            }
            String value = credentialService.getCredential(name, context);
            if (value == null) {
                return null;
            }
            headers.put(header.name(), header.prefix() + value);
        }
        return headers;
    }

    private static @Nullable HeaderCredential headerForScheme(@Nullable SecurityScheme scheme) {
        if (scheme instanceof HTTPAuthSecurityScheme http) {
            if (BEARER_SCHEME.equalsIgnoreCase(http.scheme())) {
                return new HeaderCredential(AUTHORIZATION, BEARER);
            }
            if (BASIC_SCHEME.equalsIgnoreCase(http.scheme())) {
                return new HeaderCredential(AUTHORIZATION, BASIC);
            }
        } else if (scheme instanceof OAuth2SecurityScheme || scheme instanceof OpenIdConnectSecurityScheme) {
            return new HeaderCredential(AUTHORIZATION, BEARER);
        } else if (scheme instanceof APIKeySecurityScheme apiKey
                && apiKey.location() == APIKeySecurityScheme.Location.HEADER && isSafeHeaderName(apiKey.name())) {
            return new HeaderCredential(apiKey.name(), "");
        }
        return null;
    }

    private record HeaderCredential(String name, String prefix) {}

    /**
     * Validates that a header name is safe for API key injection.
     * This prevents credential leakage by rejecting header names that could
     * be exploited to forward credentials to unintended destinations.
     * Header name comparison is case-insensitive per RFC 7230.
     *
     * @param headerName the header name to validate
     * @return true if the header name is in the safe allowlist, false otherwise
     */
    private static boolean isSafeHeaderName(String headerName) {
        return SAFE_API_KEY_HEADER_NAMES.contains(headerName.toLowerCase(Locale.ROOT));
    }
}
