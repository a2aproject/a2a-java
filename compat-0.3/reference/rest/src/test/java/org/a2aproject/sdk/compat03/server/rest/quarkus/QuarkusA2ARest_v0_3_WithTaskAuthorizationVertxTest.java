package org.a2aproject.sdk.compat03.server.rest.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.a2aproject.sdk.client.http.VertxA2AHttpClient;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth.AuthInterceptor_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerWithTaskAuthorizationTest_v0_3;
import org.a2aproject.sdk.compat03.conversion.TaskAuthorizationTestProfile_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

@QuarkusTest
@TestProfile(TaskAuthorizationTestProfile_v0_3.class)
public class QuarkusA2ARest_v0_3_WithTaskAuthorizationVertxTest extends AbstractA2AServerWithTaskAuthorizationTest_v0_3 {

    @Inject
    Vertx vertx;

    public QuarkusA2ARest_v0_3_WithTaskAuthorizationVertxTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransportWithCredentials(ClientBuilder_v0_3 builder, String username, String password) {
        AuthInterceptor_v0_3 authInterceptor = new AuthInterceptor_v0_3(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName)
                        ? getEncodedCredentials(username, password) : null);
        builder.withTransport(RestTransport_v0_3.class,
                new RestTransportConfigBuilder_v0_3()
                        .httpClient(new VertxA2AHttpClient(vertx))
                        .addInterceptor(authInterceptor));
    }
}
