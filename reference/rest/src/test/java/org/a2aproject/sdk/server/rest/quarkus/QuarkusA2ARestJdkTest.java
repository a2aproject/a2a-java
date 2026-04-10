package org.a2aproject.sdk.server.rest.quarkus;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2ARestJdkTest extends QuarkusA2ARestTest {

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class, new RestTransportConfigBuilder().httpClient(new JdkA2AHttpClient()));
    }
}
