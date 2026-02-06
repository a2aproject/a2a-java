package io.a2a.client.transport.spi.interceptors.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.spec.APIKeySecurityScheme;
import io.a2a.spec.AgentCard;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.SecurityScheme;
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
                                       @Nullable AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        Map<String, String> updatedHeaders = new HashMap<>(headers == null ? new HashMap<>() : headers);
        if (agentCard == null || agentCard.securityRequirements()== null || agentCard.securitySchemes() == null) {
            return new PayloadAndHeaders(payload, updatedHeaders);
        }
        for (Map<String, List<String>> requirement : agentCard.securityRequirements()) {
            for (String securitySchemeName : requirement.keySet()) {
                String credential = credentialService.getCredential(securitySchemeName, clientCallContext);
                if (credential != null && agentCard.securitySchemes().containsKey(securitySchemeName)) {
                    SecurityScheme securityScheme = agentCard.securitySchemes().get(securitySchemeName);
                    if (securityScheme == null) {
                        continue;
                    }
                    if (securityScheme instanceof HTTPAuthSecurityScheme httpAuthSecurityScheme) {
                        if (httpAuthSecurityScheme.scheme().toLowerCase(Locale.ROOT).equals(BEARER_SCHEME)) {
                            updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                            return new PayloadAndHeaders(payload, updatedHeaders);
                        }
                    } else if (securityScheme instanceof OAuth2SecurityScheme
                            || securityScheme instanceof OpenIdConnectSecurityScheme) {
                        updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                        return new PayloadAndHeaders(payload, updatedHeaders);
                    } else if (securityScheme instanceof APIKeySecurityScheme apiKeySecurityScheme) {
                        updatedHeaders.put(apiKeySecurityScheme.name(), credential);
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
