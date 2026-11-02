package org.a2aproject.sdk.extras.multitenancy.tests.jsonrpc;

import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.extras.multitenancy.tests.AbstractMultiTenantServerTest;
import org.a2aproject.sdk.spec.TransportProtocol;

@QuarkusTest
public class MultiTenantJSONRPCTest extends AbstractMultiTenantServerTest {

    public MultiTenantJSONRPCTest() {
        super(TEST_PORT);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:" + TEST_PORT;
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(
                JSONRPCTransport.class,
                new JSONRPCTransportConfigBuilder().httpClient(new JdkA2AHttpClient()));
    }
}
