package org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme;
import org.a2aproject.sdk.compat03.spec.OAuth2SecurityScheme;
import org.a2aproject.sdk.compat03.spec.OpenIdConnectSecurityScheme;
import org.a2aproject.sdk.compat03.spec.SecurityScheme;
import org.jspecify.annotations.Nullable;

/**
 * An interceptor that automatically adds authentication details to requests
 * based on the agent's security schemes and the credentials available.
 */
public class AuthInterceptor extends ClientCallInterceptor {

    private static final String BEARER_SCHEME = "bearer";
    public static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private final CredentialService credentialService;

    public AuthInterceptor(final CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    public PayloadAndHeaders intercept(String methodName, @Nullable Object payload, Map<String, String> headers,
                                       AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        Map<String, String> updatedHeaders = new HashMap<>(headers == null ? new HashMap<>() : headers);
        if (agentCard == null || agentCard.security() == null || agentCard.securitySchemes() == null) {
            return new PayloadAndHeaders(payload, updatedHeaders);
        }
        for (Map<String, List<String>> requirement : agentCard.security()) {
            for (String securitySchemeName : requirement.keySet()) {
                String credential = credentialService.getCredential(securitySchemeName, clientCallContext);
                if (credential != null && agentCard.securitySchemes().containsKey(securitySchemeName)) {
                    SecurityScheme securityScheme = agentCard.securitySchemes().get(securitySchemeName);
                    if (securityScheme == null) {
                        continue;
                    }
                    if (securityScheme instanceof HTTPAuthSecurityScheme httpAuthSecurityScheme) {
                        if (httpAuthSecurityScheme.getScheme().toLowerCase(Locale.ROOT).equals(BEARER_SCHEME)) {
                            updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                            return new PayloadAndHeaders(payload, updatedHeaders);
                        }
                    } else if (securityScheme instanceof OAuth2SecurityScheme
                            || securityScheme instanceof OpenIdConnectSecurityScheme) {
                        updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                        return new PayloadAndHeaders(payload, updatedHeaders);
                    } else if (securityScheme instanceof APIKeySecurityScheme apiKeySecurityScheme) {
                        updatedHeaders.put(apiKeySecurityScheme.getName(), credential);
                        return new PayloadAndHeaders(payload, updatedHeaders);
                    }
                }
            }
        }
        return new PayloadAndHeaders(payload, updatedHeaders);
    }

    private static String getBearerValue(String credential) {
        return BEARER + credential;
    }
}
