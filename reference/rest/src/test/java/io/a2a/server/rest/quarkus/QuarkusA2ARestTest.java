package io.a2a.server.rest.quarkus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.a2a.client.ClientBuilder;
import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class QuarkusA2ARestTest extends AbstractA2AServerTest {

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
    protected abstract void configureTransport(ClientBuilder builder);

    @Test
    public void testMethodNotFound() throws Exception {
        // Create the client
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        // Create the request
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/message:send"))
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .header("Content-Type", APPLICATION_JSON);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(405, response.statusCode());
        builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/message:send"))
                .DELETE()
                .header("Content-Type", APPLICATION_JSON);
        response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(405, response.statusCode());
    }
}
