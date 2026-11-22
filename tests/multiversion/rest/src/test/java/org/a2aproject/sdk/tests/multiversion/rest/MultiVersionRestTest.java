package org.a2aproject.sdk.tests.multiversion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.inject.Inject;

import com.google.gson.JsonParser;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.http.vertx.VertxA2AHttpClient;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class MultiVersionRestTest extends AbstractA2AServerTest {

    @Inject
    Vertx vertx;

    public MultiVersionRestTest() {
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
        builder.withTransport(RestTransport.class,
                new RestTransportConfigBuilder()
                        .httpClient(new VertxA2AHttpClient(vertx)));
    }
    @Test
    public void testResponseContentTypeMatchesSelectedVersion() throws Exception {
        String taskId = "versioned-content-type-test";
        saveTaskInTaskStore(Task.builder().id(taskId).contextId("versioned-content-type-context")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING)).build());
        try {
            HttpClient client = HttpClient.newHttpClient();
            for (String version : new String[] {"0.3", "1.0"}) {
                for (String id : new String[] {taskId, "missing-versioned-content-type-test"}) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(getTransportUrl() + "/v1/tasks/" + id))
                            .header("A2A-Version", version).GET().build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    assertEquals(id.equals(taskId) ? 200 : 404, response.statusCode());
                    assertEquals("0.3".equals(version) ? "application/json" : "application/a2a+json",
                            response.headers().firstValue("Content-Type").orElseThrow());
                }
            }
        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

    @Test
    public void testUnsupportedVersionBeforeMalformedHistory() throws Exception {
        HttpResponse<String> response = historyResponse("2.0", "historyLength=bad");
        assertEquals(400, response.statusCode());
        assertEquals(JsonParser.parseString("""
                {"error":{"code":-32009,"message":"Protocol version '2.0' is not supported. Supported versions: [1.0, 0.3]"}}
                """), JsonParser.parseString(response.body()));
    }

    @Test
    public void testSelectedVersionHistoryValidation() throws Exception {
        HttpResponse<String> response = historyResponse("1.0", "historyLength=bad");
        assertEquals(422, response.statusCode());
        assertEquals(JsonParser.parseString("""
                {"error":{"code":422,"status":"INVALID_ARGUMENT","message":"bad historyLength","details":[{
                "@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_PARAMS",
                "domain":"a2a-protocol.org","metadata":{}}]}}
                """), JsonParser.parseString(response.body()));
    }
    private HttpResponse<String> historyResponse(String header, String query) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/v1/tasks/missing-history-task?" + query)).GET();
        if (header != null) {
            request.header("A2A-Version", header);
        }
        HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals("1.0".equals(header) ? "application/a2a+json" : "application/json",
                response.headers().firstValue("Content-Type").orElseThrow());
        return response;
    }

}
