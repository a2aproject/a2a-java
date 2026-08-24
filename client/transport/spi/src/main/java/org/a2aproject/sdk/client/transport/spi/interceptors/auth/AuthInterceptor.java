package org.a2aproject.sdk.client.transport.spi.interceptors.auth;

import java.util.HashMap;
import java.util.List;
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
    public PayloadAndHeaders intercept(String methodName, @Nullable Object payload, Map<String, String> headers,
                                       @Nullable AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        Map<String, String> updatedHeaders = new HashMap<>(headers == null ? new HashMap<>() : headers);
        if (agentCard == null || agentCard.securityRequirements()== null || agentCard.securitySchemes() == null) {
            return new PayloadAndHeaders(payload, updatedHeaders);
        }
        for (SecurityRequirement requirement : agentCard.securityRequirements()) {
            if (requirement == null) {
                continue;
            }
            for (String securitySchemeName : requirement.schemes().keySet()) {
                String credential = credentialService.getCredential(securitySchemeName, clientCallContext);
                if (credential != null && agentCard.securitySchemes().containsKey(securitySchemeName)) {
                    SecurityScheme securityScheme = agentCard.securitySchemes().get(securitySchemeName);
                    if (securityScheme == null) {
                        continue;
                    }
                    if (securityScheme instanceof HTTPAuthSecurityScheme httpAuthSecurityScheme) {
                        String scheme = httpAuthSecurityScheme.scheme().toLowerCase(Locale.ROOT);
                        if (scheme.equals(BEARER_SCHEME)) {
                            updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                            return new PayloadAndHeaders(payload, updatedHeaders);
                        } else if (scheme.equals(BASIC_SCHEME)) {
                            updatedHeaders.put(AUTHORIZATION, getBasicValue(credential));
                            return new PayloadAndHeaders(payload, updatedHeaders);
                        }
                    } else if (securityScheme instanceof OAuth2SecurityScheme
                            || securityScheme instanceof OpenIdConnectSecurityScheme) {
                        updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                        return new PayloadAndHeaders(payload, updatedHeaders);
                    } else if (securityScheme instanceof APIKeySecurityScheme apiKeySecurityScheme) {
                        // Only inject API key if it's intended for header transport and the header name is safe
                        if (apiKeySecurityScheme.location() == APIKeySecurityScheme.Location.HEADER
                                && isSafeHeaderName(apiKeySecurityScheme.name())) {
                            updatedHeaders.put(apiKeySecurityScheme.name(), credential);
                            return new PayloadAndHeaders(payload, updatedHeaders);
                        }
                        // Skip credential injection for unsafe header names or non-header locations
                    }
                }
            }
        }
        return new PayloadAndHeaders(payload, updatedHeaders);
    }

    private static String getBearerValue(String credential) {
        return BEARER + credential;
    }

    private static String getBasicValue(String credential) {
        return BASIC + credential;
    }

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
