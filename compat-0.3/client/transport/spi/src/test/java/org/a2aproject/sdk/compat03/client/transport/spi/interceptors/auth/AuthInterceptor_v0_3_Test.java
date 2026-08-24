package org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3;
import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuth2SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuthFlows_v0_3;
import org.a2aproject.sdk.compat03.spec.OpenIdConnectSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.SecurityScheme_v0_3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthInterceptor_v0_3_Test {

    private InMemoryContextCredentialService_v0_3 credentialStore;
    private AuthInterceptor_v0_3 authInterceptor;

    @BeforeEach
    void setUp() {
        credentialStore = new InMemoryContextCredentialService_v0_3();
        authInterceptor = new AuthInterceptor_v0_3(credentialStore);
    }

    private static class HeaderInterceptor extends ClientCallInterceptor_v0_3 {
        private final String headerName;
        private final String headerValue;

        public HeaderInterceptor(String headerName, String headerValue) {
            this.headerName = headerName;
            this.headerValue = headerValue;
        }

        @Override
        public PayloadAndHeaders_v0_3 intercept(String methodName, Object payload, Map<String, String> headers,
                                                AgentCard_v0_3 agentCard, ClientCallContext_v0_3 clientCallContext) {
            Map<String, String> updatedHeaders = new HashMap<>(headers);
            updatedHeaders.put(headerName, headerValue);
            return new PayloadAndHeaders_v0_3(payload, updatedHeaders);
        }
    }

    private static class AuthTestCase {
        final String url;
        final String sessionId;
        final String schemeName;
        final String credential;
        final SecurityScheme_v0_3 securityScheme;
        final String expectedHeaderKey;
        final String expectedHeaderValue;

        AuthTestCase(String url, String sessionId, String schemeName, String credential,
                     SecurityScheme_v0_3 securityScheme, String expectedHeaderKey, String expectedHeaderValue) {
            this.url = url;
            this.sessionId = sessionId;
            this.schemeName = schemeName;
            this.credential = credential;
            this.securityScheme = securityScheme;
            this.expectedHeaderKey = expectedHeaderKey;
            this.expectedHeaderValue = expectedHeaderValue;
        }
    }

    @Test
    public void testAPIKeySecurityScheme() {
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                APIKeySecurityScheme_v0_3.TYPE,
                "secret-api-key",
                new APIKeySecurityScheme_v0_3("header", "X-API-Key", "API Key authentication"),
                "X-API-Key",
                "secret-api-key"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testAPIKeySecurityScheme_SafeHeaderName_Authorization() {
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                APIKeySecurityScheme_v0_3.TYPE,
                "secret-api-key",
                new APIKeySecurityScheme_v0_3("header", "Authorization", "API Key authentication"),
                "Authorization",
                "secret-api-key"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testAPIKeySecurityScheme_SafeHeaderName_XAuthToken() {
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                APIKeySecurityScheme_v0_3.TYPE,
                "secret-api-key",
                new APIKeySecurityScheme_v0_3("header", "X-Auth-Token", "API Key authentication"),
                "X-Auth-Token",
                "secret-api-key"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testAPIKeySecurityScheme_CaseInsensitiveHeaderName() {
        // Test that lowercase header names are accepted (case-insensitive comparison)
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                APIKeySecurityScheme_v0_3.TYPE,
                "secret-api-key",
                new APIKeySecurityScheme_v0_3("header", "x-api-key", "API Key authentication"),
                "x-api-key",
                "secret-api-key"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testAPIKeySecurityScheme_CaseInsensitiveHeaderName_Authorization() {
        // Test that lowercase "authorization" is accepted
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                APIKeySecurityScheme_v0_3.TYPE,
                "secret-api-key",
                new APIKeySecurityScheme_v0_3("header", "authorization", "API Key authentication"),
                "authorization",
                "secret-api-key"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testAPIKeySecurityScheme_UnsafeHeaderName_Rejected() {
        String sessionId = "session-id";
        String schemeName = APIKeySecurityScheme_v0_3.TYPE;
        String credential = "secret-api-key";

        credentialStore.setCredential(sessionId, schemeName, credential);

        // Use an unsafe header name that's not in the allowlist
        SecurityScheme_v0_3 securityScheme = new APIKeySecurityScheme_v0_3(
            "header",
            "X-Malicious-Redirect-Header",
            "Unsafe header"
        );
        AgentCard_v0_3 agentCard = createAgentCard(schemeName, securityScheme);

        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", sessionId), Map.of());

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
                "message/send",
                requestPayload,
                headers,
                agentCard,
                context
        );

        assertEquals(requestPayload, result.getPayload());
        // Credential should NOT be injected for unsafe header name
        assertNull(result.getHeaders().get("X-Malicious-Redirect-Header"));
        assertEquals(0, result.getHeaders().size());
    }

    @Test
    public void testAPIKeySecurityScheme_QueryLocation_NotInjectedInHeader() {
        String sessionId = "session-id";
        String schemeName = APIKeySecurityScheme_v0_3.TYPE;
        String credential = "secret-api-key";

        credentialStore.setCredential(sessionId, schemeName, credential);

        // API key in query parameter location should not be injected as header
        SecurityScheme_v0_3 securityScheme = new APIKeySecurityScheme_v0_3(
            "query",
            "api_key",
            "Query parameter API key"
        );
        AgentCard_v0_3 agentCard = createAgentCard(schemeName, securityScheme);

        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", sessionId), Map.of());

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
                "message/send",
                requestPayload,
                headers,
                agentCard,
                context
        );

        assertEquals(requestPayload, result.getPayload());
        // Credential should NOT be injected as header for query location
        assertNull(result.getHeaders().get("api_key"));
        assertEquals(0, result.getHeaders().size());
    }

    @Test
    public void testAPIKeySecurityScheme_CookieLocation_NotInjectedInHeader() {
        String sessionId = "session-id";
        String schemeName = APIKeySecurityScheme_v0_3.TYPE;
        String credential = "secret-api-key";

        credentialStore.setCredential(sessionId, schemeName, credential);

        // API key in cookie location should not be injected as header
        SecurityScheme_v0_3 securityScheme = new APIKeySecurityScheme_v0_3(
            "cookie",
            "session_token",
            "Cookie-based API key"
        );
        AgentCard_v0_3 agentCard = createAgentCard(schemeName, securityScheme);

        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", sessionId), Map.of());

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
                "message/send",
                requestPayload,
                headers,
                agentCard,
                context
        );

        assertEquals(requestPayload, result.getPayload());
        // Credential should NOT be injected as header for cookie location
        assertNull(result.getHeaders().get("session_token"));
        assertEquals(0, result.getHeaders().size());
    }

    @Test
    public void testOAuth2SecurityScheme() {
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                OAuth2SecurityScheme_v0_3.TYPE,
                "secret-oauth-access-token",
                new OAuth2SecurityScheme_v0_3(new OAuthFlows_v0_3.Builder().build(), "OAuth2 authentication", null),
                "Authorization",
                "Bearer secret-oauth-access-token"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testOidcSecurityScheme() {
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                OpenIdConnectSecurityScheme_v0_3.TYPE,
                "secret-oidc-id-token",
                new OpenIdConnectSecurityScheme_v0_3("http://provider.com/.well-known/openid-configuration", "OIDC authentication"),
                "Authorization",
                "Bearer secret-oidc-id-token"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testBearerSecurityScheme() {
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                "bearer",
                "bearer-token-123",
                new HTTPAuthSecurityScheme_v0_3(null, "bearer", "Bearer token authentication"),
                "Authorization",
                "Bearer bearer-token-123"
        );
        testSecurityScheme(authTestCase);
    }

    private void testSecurityScheme(AuthTestCase authTestCase) {
        credentialStore.setCredential(authTestCase.sessionId, authTestCase.schemeName, authTestCase.credential);

        AgentCard_v0_3 agentCard = createAgentCard(authTestCase.schemeName, authTestCase.securityScheme);
        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", authTestCase.sessionId), Map.of());

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
                "message/send",
                requestPayload,
                headers,
                agentCard,
                context
        );

        assertEquals(requestPayload, result.getPayload());
        assertEquals(authTestCase.expectedHeaderValue, result.getHeaders().get(authTestCase.expectedHeaderKey));
    }

    @Test
    void testAuthInterceptorWithoutAgentCard() {
        Map<String, Object> requestPayload = Map.of("foo", "bar");
        Map<String, String> headers = Map.of("foo", "bar");

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
                "message/send",
                requestPayload,
                headers,
                null, // no agent card
                new ClientCallContext_v0_3(Map.of(), Map.of())
        );

        // should be unchanged
        assertEquals(requestPayload, result.getPayload());
        assertEquals(headers, result.getHeaders());
    }

    @Test
    void testInMemoryContextCredentialStore() {
        String sessionId = "session-id";
        String schemeName = "test-scheme";
        String credential = "test-token";

        credentialStore.setCredential(sessionId, schemeName, credential);
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", sessionId), Map.of());
        String retrievedCredential = credentialStore.getCredential(schemeName, context);
        assertEquals(credential, retrievedCredential);

        // wrong session ID
        ClientCallContext_v0_3 wrongContext = new ClientCallContext_v0_3(Map.of("sessionId", "wrong-session"), Map.of());
        retrievedCredential = credentialStore.getCredential(schemeName, wrongContext);
        assertNull(retrievedCredential);

        retrievedCredential = credentialStore.getCredential(schemeName, null);
        assertNull(retrievedCredential);

        // no session ID in context
        ClientCallContext_v0_3 emptyContext = new ClientCallContext_v0_3(Map.of(), Map.of());
        retrievedCredential = credentialStore.getCredential(schemeName, emptyContext);
        assertNull(retrievedCredential);

        String newCredential = "new-token";
        credentialStore.setCredential(sessionId, schemeName, newCredential);
        retrievedCredential = credentialStore.getCredential(schemeName, context);
        assertEquals(newCredential, retrievedCredential);
    }

    @Test
    void testCustomInterceptor() {
        String headerName = "X-Test-Header";
        String headerValue = "Test-Value-123";
        HeaderInterceptor interceptor = new HeaderInterceptor(headerName, headerValue);

        Map<String, Object> payload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();

        PayloadAndHeaders_v0_3 result = interceptor.intercept(
                "message/send",
                payload,
                headers,
                null,
                null
        );

        assertEquals(payload, result.getPayload());
        assertEquals(headerValue, result.getHeaders().get(headerName));
    }

    @Test
    void testAvailableSecuritySchemeNotInAgentCardSecuritySchemes() {
        String schemeName = "missing";
        String sessionId = "session-id";
        String credential = "dummy-token";

        credentialStore.setCredential(sessionId, schemeName, credential);

        // Create agent card with security requirement but no scheme definition
        AgentCard_v0_3 agentCard = new AgentCard_v0_3.Builder()
            .name("missing")
            .description("Uses missing scheme definition")
            .url("http://agent.com/rpc")
            .version("1.0")
            .capabilities(new AgentCapabilities_v0_3.Builder().build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .security(List.of(Map.of(schemeName, List.of())))
            .securitySchemes(Map.of()) // no security schemes
            .build();

        Map<String, Object> requestPayload = Map.of("foo", "bar");
        Map<String, String> headers = Map.of("fizz", "buzz");
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", sessionId), Map.of());

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
            "message/send",
            requestPayload,
            headers,
            agentCard,
            context
        );

        assertEquals(requestPayload, result.getPayload());
        assertEquals(headers, result.getHeaders());
    }

    @Test
    void testNoCredentialAvailable() {
        String schemeName = "apikey";
        SecurityScheme_v0_3 securityScheme = new APIKeySecurityScheme_v0_3("header", "X-API-Key", "API Key authentication");
        AgentCard_v0_3 agentCard = createAgentCard(schemeName, securityScheme);

        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", "session-id"), Map.of());

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
            "message/send",
            requestPayload,
            headers,
            agentCard,
            context
        );

        assertEquals(requestPayload, result.getPayload());
        assertEquals(headers, result.getHeaders()); // headers should be unchanged
    }

    @Test
    void testNoAgentCardSecuritySpecified() {
        // Arrange
        AgentCard_v0_3 agentCard = new AgentCard_v0_3.Builder()
            .name("nosecuritybot")
            .description("A bot with no security requirements")
            .url("http://agent.com/rpc")
            .version("1.0")
            .capabilities(new AgentCapabilities_v0_3.Builder().build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .security(null) // no security info
            .build();

        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext_v0_3 context = new ClientCallContext_v0_3(Map.of("sessionId", "session-id"), Map.of());

        PayloadAndHeaders_v0_3 result = authInterceptor.intercept(
            "message/send",
            requestPayload,
            headers,
            agentCard,
            context
        );

        assertEquals(requestPayload, result.getPayload());
        assertEquals(headers, result.getHeaders());
    }

    /**
     * Helper method to create an AgentCard with specified security scheme.
     */
    private AgentCard_v0_3 createAgentCard(String schemeName, SecurityScheme_v0_3 securityScheme) {
        return new AgentCard_v0_3.Builder()
            .name(schemeName + "bot")
            .description("A bot that uses " + schemeName)
            .url("http://agent.com/rpc")
            .version("1.0")
            .capabilities(new AgentCapabilities_v0_3.Builder().build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .security(List.of(Map.of(schemeName, List.of())))
            .securitySchemes(Map.of(schemeName, securityScheme))
            .build();
    }
}
