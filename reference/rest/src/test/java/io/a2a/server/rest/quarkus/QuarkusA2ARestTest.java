package io.a2a.server.rest.quarkus;

import static io.a2a.server.apps.common.AbstractA2AServerTest.APPLICATION_JSON;

import io.a2a.client.ClientBuilder;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfigBuilder;
import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.spec.TransportProtocol;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QuarkusA2ARestTest extends AbstractA2AServerTest {

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
        Assertions.assertEquals(405, response.statusCode());
        builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/v1/message:send"))
                .DELETE()
                .header("Content-Type", APPLICATION_JSON);
        response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(405, response.statusCode());
    }
}
