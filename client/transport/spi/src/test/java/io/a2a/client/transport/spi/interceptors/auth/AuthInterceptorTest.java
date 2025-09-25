package io.a2a.client.transport.spi.interceptors.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.spec.APIKeySecurityScheme;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OAuthFlows;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.SecurityScheme;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthInterceptorTest {

    private InMemoryContextCredentialService credentialStore;
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setUp() {
        credentialStore = new InMemoryContextCredentialService();
        authInterceptor = new AuthInterceptor(credentialStore);
    }

    private static class HeaderInterceptor extends ClientCallInterceptor {
        private final String headerName;
        private final String headerValue;

        public HeaderInterceptor(String headerName, String headerValue) {
            this.headerName = headerName;
            this.headerValue = headerValue;
        }

        @Override
        public PayloadAndHeaders intercept(String methodName, Object payload, Map<String, String> headers,
                                           AgentCard agentCard, ClientCallContext clientCallContext) {
            Map<String, String> updatedHeaders = new HashMap<>(headers);
            updatedHeaders.put(headerName, headerValue);
            return new PayloadAndHeaders(payload, updatedHeaders);
        }
    }

    private static class AuthTestCase {
        final String url;
        final String sessionId;
        final String schemeName;
        final String credential;
        final SecurityScheme securityScheme;
        final String expectedHeaderKey;
        final String expectedHeaderValue;

        AuthTestCase(String url, String sessionId, String schemeName, String credential,
                    SecurityScheme securityScheme, String expectedHeaderKey, String expectedHeaderValue) {
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
                APIKeySecurityScheme.API_KEY,
                "secret-api-key",
                new APIKeySecurityScheme("header", "x-api-key", "API Key authentication"),
                "x-api-key",
                "secret-api-key"
        );
        testSecurityScheme(authTestCase);
    }

    @Test
    public void testOAuth2SecurityScheme() {
        AuthTestCase authTestCase = new AuthTestCase(
                "http://agent.com/rpc",
                "session-id",
                OAuth2SecurityScheme.OAUTH2,
                "secret-oauth-access-token",
                new OAuth2SecurityScheme(new OAuthFlows.Builder().build(), "OAuth2 authentication", null),
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
                OpenIdConnectSecurityScheme.OPENID_CONNECT,
                "secret-oidc-id-token",
                new OpenIdConnectSecurityScheme("http://provider.com/.well-known/openid-configuration", "OIDC authentication"),
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
                new HTTPAuthSecurityScheme(null, "bearer", "Bearer token authentication"),
                "Authorization",
                "Bearer bearer-token-123"
        );
        testSecurityScheme(authTestCase);
    }

    private void testSecurityScheme(AuthTestCase authTestCase) {
        credentialStore.setCredential(authTestCase.sessionId, authTestCase.schemeName, authTestCase.credential);

        AgentCard agentCard = createAgentCard(authTestCase.schemeName, authTestCase.securityScheme);
        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext context = new ClientCallContext(Map.of("sessionId", authTestCase.sessionId), Map.of());

        PayloadAndHeaders result = authInterceptor.intercept(
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

        PayloadAndHeaders result = authInterceptor.intercept(
                "message/send",
                requestPayload,
                headers,
                null, // no agent card
                new ClientCallContext(Map.of(), Map.of())
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
        ClientCallContext context = new ClientCallContext(Map.of("sessionId", sessionId), Map.of());
        String retrievedCredential = credentialStore.getCredential(schemeName, context);
        assertEquals(credential, retrievedCredential);

        // wrong session ID
        ClientCallContext wrongContext = new ClientCallContext(Map.of("sessionId", "wrong-session"), Map.of());
        retrievedCredential = credentialStore.getCredential(schemeName, wrongContext);
        assertNull(retrievedCredential);

        retrievedCredential = credentialStore.getCredential(schemeName, null);
        assertNull(retrievedCredential);

        // no session ID in context
        ClientCallContext emptyContext = new ClientCallContext(Map.of(), Map.of());
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

        PayloadAndHeaders result = interceptor.intercept(
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
        AgentCard agentCard = new AgentCard.Builder()
            .name("missing")
            .description("Uses missing scheme definition")
            .url("http://agent.com/rpc")
            .version("1.0")
            .capabilities(new AgentCapabilities.Builder().build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .security(List.of(Map.of(schemeName, List.of())))
            .securitySchemes(Map.of()) // no security schemes
            .build();
            
        Map<String, Object> requestPayload = Map.of("foo", "bar");
        Map<String, String> headers = Map.of("fizz", "buzz");
        ClientCallContext context = new ClientCallContext(Map.of("sessionId", sessionId), Map.of());

        PayloadAndHeaders result = authInterceptor.intercept(
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
        SecurityScheme securityScheme = new APIKeySecurityScheme("header", "X-API-Key", "API Key authentication");
        AgentCard agentCard = createAgentCard(schemeName, securityScheme);
        
        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext context = new ClientCallContext(Map.of("sessionId", "session-id"), Map.of());

        PayloadAndHeaders result = authInterceptor.intercept(
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
        AgentCard agentCard = new AgentCard.Builder()
            .name("nosecuritybot")
            .description("A bot with no security requirements")
            .url("http://agent.com/rpc")
            .version("1.0")
            .capabilities(new AgentCapabilities.Builder().build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .security(null) // no security info
            .build();
            
        Map<String, Object> requestPayload = Map.of("test", "payload");
        Map<String, String> headers = Map.of();
        ClientCallContext context = new ClientCallContext(Map.of("sessionId", "session-id"), Map.of());

        PayloadAndHeaders result = authInterceptor.intercept(
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
    private AgentCard createAgentCard(String schemeName, SecurityScheme securityScheme) {
        return new AgentCard.Builder()
            .name(schemeName + "bot")
            .description("A bot that uses " + schemeName)
            .url("http://agent.com/rpc")
            .version("1.0")
            .capabilities(new AgentCapabilities.Builder().build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .security(List.of(Map.of(schemeName, List.of())))
            .securitySchemes(Map.of(schemeName, securityScheme))
            .build();
    }
}
