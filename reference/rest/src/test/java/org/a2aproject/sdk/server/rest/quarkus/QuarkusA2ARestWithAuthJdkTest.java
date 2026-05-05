package org.a2aproject.sdk.server.rest.quarkus;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerWithAuthTest;
import org.a2aproject.sdk.server.apps.common.AuthTestProfile;
import org.a2aproject.sdk.spec.TransportProtocol;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

/**
 * Authentication tests for REST (HTTP JSON) transport with JDK HTTP client.
 * <p>
 * Validates that:
 * <ul>
 *   <li>{@code @Authenticated} annotations are enforced</li>
 *   <li>{@code @PermitAll} on getAgentCard() allows public access</li>
 *   <li>Protected endpoints require valid credentials</li>
 * </ul>
 */
@QuarkusTest
@TestProfile(AuthTestProfile.class)
public class QuarkusA2ARestWithAuthJdkTest extends AbstractA2AServerWithAuthTest {

    public QuarkusA2ARestWithAuthJdkTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransportWithAuth(ClientBuilder builder) {
        // Use AuthInterceptor with inline CredentialService
        AuthInterceptor authInterceptor = new AuthInterceptor(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName) ? getEncodedCredentials() : null
        );

        builder.withTransport(RestTransport.class,
                new RestTransportConfigBuilder()
                        .httpClient(new JdkA2AHttpClient())
                        .addInterceptor(authInterceptor));
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        // No auth (for unauthenticated client creation)
        builder.withTransport(RestTransport.class,
                new RestTransportConfigBuilder()
                        .httpClient(new JdkA2AHttpClient()));
    }

    @Test
    @Override
    public void testBasicAuthWorksViaHttp() throws Exception {
        // Override: the base test posts JSON-RPC to "/", which is only
        // a valid route in the JSON-RPC module. For REST, use a GET endpoint.
        saveTaskInTaskStore(MINIMAL_TASK);

        givenAuthenticated()
                .get("/tasks/" + MINIMAL_TASK.id())
                .then()
                .statusCode(200);

        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }
}
