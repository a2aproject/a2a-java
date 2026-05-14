package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.VertxA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.spec.TransportProtocol;

@QuarkusTest
public class MultiVersionJSONRPCTest extends AbstractA2AServerTest {

    @Inject
    Vertx vertx;

    public MultiVersionJSONRPCTest() {
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
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class,
                new JSONRPCTransportConfigBuilder()
                        .httpClient(new VertxA2AHttpClient(vertx)));
    }
}
