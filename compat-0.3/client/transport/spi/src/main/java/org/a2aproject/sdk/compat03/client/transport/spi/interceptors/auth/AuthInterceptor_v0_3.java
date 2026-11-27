package org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3;
import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuth2SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OpenIdConnectSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.SecurityScheme_v0_3;
import org.jspecify.annotations.Nullable;

/**
 * An interceptor that automatically adds authentication details to requests
 * based on the agent's security schemes and the credentials available.
 */
public class AuthInterceptor_v0_3 extends ClientCallInterceptor_v0_3 {

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

    private final CredentialService_v0_3 credentialService;

    public AuthInterceptor_v0_3(final CredentialService_v0_3 credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    public PayloadAndHeaders_v0_3 intercept(String methodName, Object payload, Map<String, String> headers,
                                            AgentCard_v0_3 agentCard, @Nullable ClientCallContext_v0_3 clientCallContext) {
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
        return new PayloadAndHeaders_v0_3(payload, updatedHeaders);
    }

    private @Nullable Map<String, String> resolveCredentials(AgentCard_v0_3 agentCard,
            @Nullable ClientCallContext_v0_3 clientCallContext) {
        if (agentCard.security() == null || agentCard.security().isEmpty()) {
            return Map.of();
        }
        var schemes = agentCard.securitySchemes();
        for (Map<String, List<String>> requirement : agentCard.security()) {
            if (requirement == null) {
                continue;
            }
            Map<String, String> headers = resolveAlternative(requirement.keySet(), schemes,
                    clientCallContext);
            if (headers != null) {
                return headers;
            }
        }
        return null;
    }

    private @Nullable Map<String, String> resolveAlternative(Set<String> names,
            @Nullable Map<String, SecurityScheme_v0_3> schemes,
            @Nullable ClientCallContext_v0_3 context) {
        Map<String, String> headers = new HashMap<>();
        Set<String> headerNames = new HashSet<>();
        for (String name : names) {
            SecurityScheme_v0_3 scheme = schemes == null ? null : schemes.get(name);
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

    private static @Nullable HeaderCredential headerForScheme(@Nullable SecurityScheme_v0_3 scheme) {
        if (scheme instanceof HTTPAuthSecurityScheme_v0_3 http) {
            if (BEARER_SCHEME.equalsIgnoreCase(http.scheme())) {
                return new HeaderCredential(AUTHORIZATION, BEARER);
            }
            if (BASIC_SCHEME.equalsIgnoreCase(http.scheme())) {
                return new HeaderCredential(AUTHORIZATION, BASIC);
            }
        } else if (scheme instanceof OAuth2SecurityScheme_v0_3 || scheme instanceof OpenIdConnectSecurityScheme_v0_3) {
            return new HeaderCredential(AUTHORIZATION, BEARER);
        } else if (scheme instanceof APIKeySecurityScheme_v0_3 apiKey
                && "header".equals(apiKey.in()) && isSafeHeaderName(apiKey.name())) {
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
