package org.a2aproject.sdk.server.rest.quarkus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

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
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testSubscribeToTaskWithGetOpensEventStream() throws Exception {
        String taskId = "rest-subscribe-get-test";
        String contextId = "rest-subscribe-get-context";
        saveTaskInTaskStore(Task.builder().id(taskId).contextId(contextId)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING)).build());
        try {
            ensureQueueForTask(taskId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getTransportUrl() + "/tasks/" + taskId + ":subscribe"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "text/event-stream")
                    .header("A2A-Version", "1.0")
                    .GET().build();
            HttpResponse<InputStream> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                Assertions.assertEquals(200, response.statusCode());
                Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("")
                        .startsWith("text/event-stream"));
                BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8));
                String line;
                boolean initialEvent = false;
                // The stream begins with an SSE comment frame; skip it and its blank separator.
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        initialEvent = true;
                        break;
                    }
                }
                Assertions.assertTrue(initialEvent, "Expected an initial SSE task event");

                enqueueEventOnServer(TaskStatusUpdateEvent.builder().taskId(taskId).contextId(contextId)
                        .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED)).build());
                boolean completed = false;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:") && line.contains("TASK_STATE_COMPLETED")) {
                        completed = true;
                        break;
                    }
                }
                Assertions.assertTrue(completed, "Expected a completed task event");
            }
        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

    @Test
    public void testRestResponsesUseA2aJsonContentType() throws Exception {
        String taskId = "rest-content-type-test";
        saveTaskInTaskStore(Task.builder().id(taskId).contextId("rest-content-type-context")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING)).build());
        try {
            HttpClient client = HttpClient.newHttpClient();
            for (String id : new String[] { taskId, "missing-rest-content-type-test" }) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getTransportUrl() + "/tasks/" + id))
                        .header("A2A-Version", "1.0")
                        .GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                Assertions.assertEquals(id.equals(taskId) ? 200 : 404, response.statusCode());
                Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("")
                        .startsWith("application/a2a+json"));
            }
        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

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
