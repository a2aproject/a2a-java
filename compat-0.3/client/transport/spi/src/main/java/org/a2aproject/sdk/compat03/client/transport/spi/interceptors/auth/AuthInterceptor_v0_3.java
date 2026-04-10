package org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private final CredentialService_v0_3 credentialService;

    public AuthInterceptor_v0_3(final CredentialService_v0_3 credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    public PayloadAndHeaders_v0_3 intercept(String methodName, @Nullable Object payload, Map<String, String> headers,
                                            AgentCard_v0_3 agentCard, @Nullable ClientCallContext_v0_3 clientCallContext) {
        Map<String, String> updatedHeaders = new HashMap<>(headers == null ? new HashMap<>() : headers);
        if (agentCard == null || agentCard.security() == null || agentCard.securitySchemes() == null) {
            return new PayloadAndHeaders_v0_3(payload, updatedHeaders);
        }
        for (Map<String, List<String>> requirement : agentCard.security()) {
            for (String securitySchemeName : requirement.keySet()) {
                String credential = credentialService.getCredential(securitySchemeName, clientCallContext);
                if (credential != null && agentCard.securitySchemes().containsKey(securitySchemeName)) {
                    SecurityScheme_v0_3 securityScheme = agentCard.securitySchemes().get(securitySchemeName);
                    if (securityScheme == null) {
                        continue;
                    }
                    if (securityScheme instanceof HTTPAuthSecurityScheme_v0_3 httpAuthSecurityScheme) {
                        if (httpAuthSecurityScheme.getScheme().toLowerCase(Locale.ROOT).equals(BEARER_SCHEME)) {
                            updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                            return new PayloadAndHeaders_v0_3(payload, updatedHeaders);
                        }
                    } else if (securityScheme instanceof OAuth2SecurityScheme_v0_3
                            || securityScheme instanceof OpenIdConnectSecurityScheme_v0_3) {
                        updatedHeaders.put(AUTHORIZATION, getBearerValue(credential));
                        return new PayloadAndHeaders_v0_3(payload, updatedHeaders);
                    } else if (securityScheme instanceof APIKeySecurityScheme_v0_3 apiKeySecurityScheme) {
                        updatedHeaders.put(apiKeySecurityScheme.getName(), credential);
                        return new PayloadAndHeaders_v0_3(payload, updatedHeaders);
                    }
                }
            }
        }
        return new PayloadAndHeaders_v0_3(payload, updatedHeaders);
    }

    private static String getBearerValue(String credential) {
        return BEARER + credential;
    }
}
