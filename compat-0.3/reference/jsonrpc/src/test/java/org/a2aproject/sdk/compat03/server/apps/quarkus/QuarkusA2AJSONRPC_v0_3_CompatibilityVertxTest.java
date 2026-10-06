package org.a2aproject.sdk.compat03.server.apps.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.vertx.VertxA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerCompatibilityTest_v0_3;

@QuarkusTest
@TestProfile(CompatibilityTestProfile_v0_3.class)
public class QuarkusA2AJSONRPC_v0_3_CompatibilityVertxTest extends AbstractA2AServerCompatibilityTest_v0_3 {
    @Inject Vertx vertx;
    public QuarkusA2AJSONRPC_v0_3_CompatibilityVertxTest() { super(8081); }
    @Override protected String getTransportProtocol() { return "JSONRPC"; }
    @Override protected String getTransportUrl() { return "http://localhost:8081"; }
    @Override protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                .httpClient(new VertxA2AHttpClient(vertx)));
    }
}
