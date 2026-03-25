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
    public void testSendMessageWithUnsupportedContentType() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/message:send"))
                .POST(HttpRequest.BodyPublishers.ofString("test body"))
                .header("Content-Type", "text/plain")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(415, response.statusCode());
        Assertions.assertTrue(response.body().contains("CONTENT_TYPE_NOT_SUPPORTED"),
                "Expected CONTENT_TYPE_NOT_SUPPORTED in response body: " + response.body());
    }

    @Test
    public void testSendMessageWithUnsupportedProtocolVersion() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/message:send"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", APPLICATION_JSON)
                .header("A2A-Version", "0.4.0")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.body().contains("VERSION_NOT_SUPPORTED"),
                "Expected VERSION_NOT_SUPPORTED in response body: " + response.body());
    }

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
