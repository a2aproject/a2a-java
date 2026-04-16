package org.a2aproject.sdk.compat03.server.rest.quarkus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerServerTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class QuarkusA2ARest_v0_3_Test extends AbstractA2AServerServerTest_v0_3 {

    public QuarkusA2ARest_v0_3_Test() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(RestTransport_v0_3.class, new RestTransportConfigBuilder_v0_3());
    }

    @Test
    public void testMethodNotFound() throws Exception {
        // Create the client
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        // Create the request
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/v1/message:send"))
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .header("Content-Type", APPLICATION_JSON);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
        builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/v1/message:send"))
                .DELETE()
                .header("Content-Type", APPLICATION_JSON);
        response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }
}
