package org.a2aproject.sdk.compat03.server.apps.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerCompatibilityWithAuthTest_v0_3;

@QuarkusTest
@TestProfile(CompatibilityAuthTestProfile_v0_3.class)
public class QuarkusA2AJSONRPC_v0_3_WithAuthCompatibilityTest
        extends AbstractA2AServerCompatibilityWithAuthTest_v0_3 {
    public QuarkusA2AJSONRPC_v0_3_WithAuthCompatibilityTest() { super(8081); }
    @Override protected String getTransportProtocol() { return "JSONRPC"; }
    @Override protected String getTransportUrl() { return "http://localhost:8081"; }
    @Override protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                .httpClient(new JdkA2AHttpClient()));
    }
    @Override protected void configureTransportWithAuth(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                .httpClient(new JdkA2AHttpClient()).addInterceptor(new AuthInterceptor(
                        (scheme, context) -> BASIC_AUTH_SCHEME_NAME.equals(scheme) ? getEncodedCredentials() : null)));
    }
}
