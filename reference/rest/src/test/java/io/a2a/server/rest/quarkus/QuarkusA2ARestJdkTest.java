package io.a2a.server.rest.quarkus;

import io.a2a.client.ClientBuilder;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfigBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2ARestJdkTest extends QuarkusA2ARestTest {

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class, new RestTransportConfigBuilder().httpClient(new JdkA2AHttpClient()));
    }
}
