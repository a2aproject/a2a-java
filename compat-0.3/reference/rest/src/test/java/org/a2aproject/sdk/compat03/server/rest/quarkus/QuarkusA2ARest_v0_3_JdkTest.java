package org.a2aproject.sdk.compat03.server.rest.quarkus;

import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransportConfigBuilder_v0_3;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusA2ARest_v0_3_JdkTest extends QuarkusA2ARest_v0_3_Test {

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(RestTransport_v0_3.class,
                new RestTransportConfigBuilder_v0_3().httpClient(new JdkA2AHttpClient_v0_3()));
    }
}
