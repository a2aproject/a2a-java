package org.a2aproject.sdk.tests.multiversion.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.VertxA2AHttpClient;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerWithAuthTest;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AuthTestProfile.class)
public class MultiVersionRestWithAuthTest extends AbstractA2AServerWithAuthTest {

    @Inject
    Vertx vertx;

    public MultiVersionRestWithAuthTest() {
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
                        .httpClient(new VertxA2AHttpClient(vertx))
                        .addInterceptor(authInterceptor));
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class,
                new RestTransportConfigBuilder()
                        .httpClient(new VertxA2AHttpClient(vertx)));
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
