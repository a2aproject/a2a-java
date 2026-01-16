package io.a2a.server.apps.quarkus;

import io.a2a.client.ClientBuilder;
import io.a2a.client.http.VertxA2AHttpClient;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;

@QuarkusTest
public class QuarkusA2AJSONRPCVertxTest extends QuarkusA2AJSONRPCTest {

    @Inject
    Vertx vertx;

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder().httpClient(new VertxA2AHttpClient(vertx)));
    }
}
