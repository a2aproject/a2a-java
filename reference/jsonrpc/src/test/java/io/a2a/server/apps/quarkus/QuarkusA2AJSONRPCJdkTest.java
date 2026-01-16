package io.a2a.server.apps.quarkus;

import io.a2a.client.ClientBuilder;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2AJSONRPCJdkTest extends QuarkusA2AJSONRPCTest {

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder().httpClient(new JdkA2AHttpClient()));
    }
}
