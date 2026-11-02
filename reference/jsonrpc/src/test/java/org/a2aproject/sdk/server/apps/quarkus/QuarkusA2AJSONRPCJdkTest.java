package org.a2aproject.sdk.server.apps.quarkus;

import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;

@QuarkusTest
public class QuarkusA2AJSONRPCJdkTest extends QuarkusA2AJSONRPCTest {

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(
                JSONRPCTransport.class,
                new JSONRPCTransportConfigBuilder().httpClient(new JdkA2AHttpClient()));
    }
}
