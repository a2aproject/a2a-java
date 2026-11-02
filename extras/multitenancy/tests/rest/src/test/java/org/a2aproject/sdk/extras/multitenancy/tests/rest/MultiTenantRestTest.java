package org.a2aproject.sdk.extras.multitenancy.tests.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.extras.multitenancy.tests.AbstractMultiTenantServerTest;
import org.a2aproject.sdk.spec.TransportProtocol;

@QuarkusTest
public class MultiTenantRestTest extends AbstractMultiTenantServerTest {

    public MultiTenantRestTest() {
        super(TEST_PORT);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:" + TEST_PORT;
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(
                RestTransport.class,
                new RestTransportConfigBuilder().httpClient(new JdkA2AHttpClient()));
    }
}
