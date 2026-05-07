package org.a2aproject.sdk.server.rest.quarkus;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.AndroidA2AHttpClient;
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
 * Authentication tests for REST (HTTP JSON) transport with Android HTTP client.
 */
@QuarkusTest
@TestProfile(AuthTestProfile.class)
public class QuarkusA2ARestWithAuthAndroidTest extends AbstractA2AServerWithAuthTest {

    public QuarkusA2ARestWithAuthAndroidTest() {
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
        AuthInterceptor authInterceptor = new AuthInterceptor(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName) ? getEncodedCredentials() : null
        );

        builder.withTransport(RestTransport.class,
                new RestTransportConfigBuilder()
                        .httpClient(new AndroidA2AHttpClient())
                        .addInterceptor(authInterceptor));
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class,
                new RestTransportConfigBuilder()
                        .httpClient(new AndroidA2AHttpClient()));
    }

    @Test
    @Override
    public void testBasicAuthWorksViaHttp() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);

        givenAuthenticated()
                .get("/tasks/" + MINIMAL_TASK.id())
                .then()
                .statusCode(200);

        deleteTaskInTaskStore(MINIMAL_TASK.id());
    }
}
