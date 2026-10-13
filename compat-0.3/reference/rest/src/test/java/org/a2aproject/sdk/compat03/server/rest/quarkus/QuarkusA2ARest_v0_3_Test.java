package org.a2aproject.sdk.compat03.server.rest.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerServerTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import org.junit.jupiter.api.Test;

public abstract class QuarkusA2ARest_v0_3_Test extends AbstractA2AServerServerTest_v0_3 {

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
    protected abstract void configureTransport(ClientBuilder_v0_3 builder);

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
    @Test
    public void testHistoryAliasWireValidation() throws Exception {
        String[][] cases = {
                {"", "404", "TaskNotFoundError", "Task not found"},
                {"history_length=", "422", "InvalidParamsError", "bad history_length or historyLength"},
                {"historyLength=", "422", "InvalidParamsError", "bad history_length or historyLength"},
                {"history_length=&historyLength=bad", "422", "InvalidParamsError",
                        "Only one of 'history_length' or 'historyLength' may be specified"},
                {"history_length=-1", "500", "InternalError", "Invalid history length"},
                {"history_length=2", "404", "TaskNotFoundError", "Task not found"},
                {"historyLength=2", "404", "TaskNotFoundError", "Task not found"},
                {"history_length=2&historyLength=bad", "422", "InvalidParamsError",
                        "Only one of 'history_length' or 'historyLength' may be specified"},
                {"history_length=bad", "422", "InvalidParamsError", "bad history_length or historyLength"},
                {"historyLength=bad", "422", "InvalidParamsError", "bad history_length or historyLength"}
        };
        HttpClient client = HttpClient.newHttpClient();
        for (String[] entry : cases) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + serverPort + "/v1/tasks/missing-alias-task?" + entry[0]))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(Integer.parseInt(entry[1]), response.statusCode(), response.body());
            assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
            assertEquals("{\"error\": \"org.a2aproject.sdk.compat03.spec." + entry[2]
                    + "_v0_3\", \"message\": \"" + entry[3] + "\"}", response.body());
        }
    }

}
