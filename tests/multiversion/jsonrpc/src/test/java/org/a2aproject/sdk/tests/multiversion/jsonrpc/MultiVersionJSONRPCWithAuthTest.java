package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.VertxA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerWithAuthTest;
import org.a2aproject.sdk.spec.TransportProtocol;

@QuarkusTest
@TestProfile(AuthTestProfile.class)
public class MultiVersionJSONRPCWithAuthTest extends AbstractA2AServerWithAuthTest {

    @Inject
    Vertx vertx;

    public MultiVersionJSONRPCWithAuthTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
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

        builder.withTransport(JSONRPCTransport.class,
                new JSONRPCTransportConfigBuilder()
                        .httpClient(new VertxA2AHttpClient(vertx))
                        .addInterceptor(authInterceptor));
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class,
                new JSONRPCTransportConfigBuilder()
                        .httpClient(new VertxA2AHttpClient(vertx)));
    }
}
