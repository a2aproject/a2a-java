package io.a2a.server.rest.quarkus;

import io.a2a.client.ClientBuilder;
import io.a2a.client.http.VertxA2AHttpClient;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfigBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;

@QuarkusTest
public class QuarkusA2ARestVertxTest extends QuarkusA2ARestTest {

    @Inject
    Vertx vertx;

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class, new RestTransportConfigBuilder().httpClient(new VertxA2AHttpClient(vertx)));
    }
}
