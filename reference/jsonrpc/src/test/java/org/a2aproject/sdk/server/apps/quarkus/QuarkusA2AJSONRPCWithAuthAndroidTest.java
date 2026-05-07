package org.a2aproject.sdk.server.apps.quarkus;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.AndroidA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerWithAuthTest;
import org.a2aproject.sdk.server.apps.common.AuthTestProfile;
import org.a2aproject.sdk.spec.TransportProtocol;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Authentication tests for JSON-RPC transport with Android HTTP client.
 */
@QuarkusTest
@TestProfile(AuthTestProfile.class)
public class QuarkusA2AJSONRPCWithAuthAndroidTest extends AbstractA2AServerWithAuthTest {

    public QuarkusA2AJSONRPCWithAuthAndroidTest() {
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

        JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder()
                .httpClient(new AndroidA2AHttpClient())
                .addInterceptor(authInterceptor)
                .build();

        builder.withTransport(JSONRPCTransport.class, config);
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class,
                new JSONRPCTransportConfigBuilder()
                        .httpClient(new AndroidA2AHttpClient()));
    }
}
