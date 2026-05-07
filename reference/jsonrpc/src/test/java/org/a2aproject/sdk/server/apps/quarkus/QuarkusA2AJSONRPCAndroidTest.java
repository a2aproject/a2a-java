package org.a2aproject.sdk.server.apps.quarkus;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.AndroidA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2AJSONRPCAndroidTest extends QuarkusA2AJSONRPCTest {

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder().httpClient(new AndroidA2AHttpClient()));
    }
}
