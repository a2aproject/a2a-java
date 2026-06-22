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
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerWithTaskAuthorizationTest;
import org.a2aproject.sdk.spec.TransportProtocol;

@QuarkusTest
@TestProfile(TaskAuthorizationTestProfile.class)
public class MultiVersionRestWithTaskAuthorizationTest extends AbstractA2AServerWithTaskAuthorizationTest {

    @Inject
    Vertx vertx;

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransportWithCredentials(ClientBuilder builder, String username, String password) {
        AuthInterceptor authInterceptor = new AuthInterceptor(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName)
                        ? getEncodedCredentials(username, password) : null);
        builder.withTransport(RestTransport.class,
                new RestTransportConfigBuilder()
                        .httpClient(new VertxA2AHttpClient(vertx))
                        .addInterceptor(authInterceptor));
    }
}
