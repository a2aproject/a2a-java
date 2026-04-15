package org.a2aproject.sdk.compat03.server.rest.quarkus;

import org.a2aproject.sdk.compat03.client.ClientBuilder;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.compat03.conversion.AbstractCompat03ServerTest;
import org.a2aproject.sdk.compat03.spec.TransportProtocol;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2ARestTest extends AbstractCompat03ServerTest {

    public QuarkusA2ARestTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class, new RestTransportConfigBuilder());
    }
}
