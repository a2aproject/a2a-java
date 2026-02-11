package io.a2a.extras.opentelemetry.it;

import static io.quarkus.vertx.web.ReactiveRoutes.APPLICATION_JSON;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.*;
import io.opentelemetry.api.trace.SpanKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for OpenTelemetry A2A integration tests.
 * Contains common test logic shared between unit and integration test modes.
 */
abstract class OpenTelemetryA2ABaseTest extends BaseTest {

    protected Client client;
    protected final int serverPort = 8081;

    @BeforeEach
    void setUp() throws A2AClientException {
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setStreaming(false)
                .build();

        client = Client.builder(A2A.getAgentCard("http://localhost:" + serverPort))
                .clientConfig(clientConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();
    }

    @BeforeEach
    void reset() {
        await().atMost(5, SECONDS).until(() -> {
            List<Map<String, Object>> spans = getSpans();
            if (spans.isEmpty()) {
                return true;
            } else {
                given().get("/reset").then().statusCode(HTTP_OK);
                return false;
            }
        });
    }

    @Test
    void testGetTaskCreatesSpans() throws Exception {
        String taskId = "span-test-task-1";
        String contextId = "span-test-ctx-1";

        Task task = Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.COMPLETED))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        reset();

        try {
            Task retrievedTask = client.getTask(new TaskQueryParams(taskId), null);

            assertNotNull(retrievedTask);
            assertEquals(taskId, retrievedTask.id());

            Thread.sleep(5000);

            List<Map<String, Object>> spans = getSpans();
            System.out.println("We have created spans " + spans);
            assertFalse(spans.isEmpty(), "Should have created spans for getTask operation");

            long serverSpanCount = spans.stream()
                    .filter(span -> SpanKind.valueOf((span.get("kind").toString())) == SpanKind.SERVER)
                    .count();
            assertTrue(serverSpanCount > 0, "Should have at least one SERVER span");

            Map<String, Object> serverSpan = spans.stream()
                    .filter(span -> SpanKind.valueOf(span.get("kind").toString()) == SpanKind.SERVER)
                    .filter(span -> "GetTask".equals(span.get("name")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No SERVER span found for GetTask"));

            assertEquals("GetTask", serverSpan.get("attr_gen_ai.agent.a2a.operation.name"),
                    "Operation name attribute should be set");
            assertEquals(taskId, serverSpan.get("attr_gen_ai.agent.a2a.task_id"),
                    "Task ID attribute should be set");

        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

    @Test
    void testListTasksCreatesSpans() throws Exception {
        reset();

        ListTasksParams params = new ListTasksParams(
                null, null, null, null, null, null, null, ""
        );

        io.a2a.jsonrpc.common.wrappers.ListTasksResult result = client.listTasks(params, null);

        assertNotNull(result);

        Thread.sleep(5000);

        List<Map<String, Object>> spans = getSpans();
        System.out.println("We have created spans " + spans);
        assertFalse(spans.isEmpty(), "Should have created spans for listTasks");

        Map<String, Object> serverSpan = spans.stream()
                .filter(span -> SpanKind.valueOf(span.get("kind").toString()) == SpanKind.SERVER)
                .filter(span -> "ListTasks".equals(span.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No SERVER span found for ListTasks"));

        assertEquals("ListTasks", serverSpan.get("attr_gen_ai.agent.a2a.operation.name"),
                "Operation name attribute should be set");
    }

    @Test
    void testCancelTaskCreatesSpans() throws Exception {
        String taskId = "cancel-test-task-1";
        String contextId = "cancel-test-ctx-1";

        Task task = Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        ensureQueueForTask(taskId);
        reset();

        try {
            Task cancelledTask = client.cancelTask(new TaskIdParams(taskId), null);

            assertNotNull(cancelledTask);
            assertEquals(TaskState.CANCELED, cancelledTask.status().state());

            Thread.sleep(5000);

            List<Map<String, Object>> spans = getSpans();
            System.out.println("We have created spans " + spans);
            assertFalse(spans.isEmpty(), "Should have created spans for cancelTask");

            Map<String, Object> serverSpan = spans.stream()
                    .filter(span -> SpanKind.valueOf(span.get("kind").toString()) == SpanKind.SERVER)
                    .filter(span -> "CancelTask".equals(span.get("name")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No SERVER span found for CancelTask"));

            assertEquals("CancelTask", serverSpan.get("attr_gen_ai.agent.a2a.operation.name"),
                    "Operation name attribute should be set");
            assertEquals(taskId, serverSpan.get("attr_gen_ai.agent.a2a.task_id"),
                    "Task ID attribute should be set");

        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

    @Test
    void testSpanAttributes() throws Exception {
        String taskId = "attr-test-task-1";
        String contextId = "attr-test-ctx-1";

        Task task = Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.COMPLETED))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        reset();

        try {
            client.getTask(new TaskQueryParams(taskId), null);

            Thread.sleep(5000);

            List<Map<String, Object>> spans = getSpans();
            System.out.println("We have created spans " + spans);
            assertFalse(spans.isEmpty());

            Map<String, Object> serverSpan = spans.stream()
                    .filter(span -> SpanKind.valueOf(span.get("kind").toString()) == SpanKind.SERVER)
                    .filter(span -> "GetTask".equals(span.get("name")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No SERVER span found for GetTask"));

            assertNotNull(serverSpan.get("spanId"), "Span should have a span ID");
            assertNotNull(serverSpan.get("traceId"), "Span should have a trace ID");
            assertEquals("GetTask", serverSpan.get("name"), "Span name should be GetTask");
            assertEquals("SERVER", serverSpan.get("kind"), "Span kind should be SERVER");
            assertEquals(Boolean.TRUE, serverSpan.get("ended"), "Span should be ended");

            assertEquals("GetTask", serverSpan.get("attr_gen_ai.agent.a2a.operation.name"),
                    "Operation name attribute should be set");
            assertEquals(taskId, serverSpan.get("attr_gen_ai.agent.a2a.task_id"),
                    "Task ID attribute should be set");

            assertNotNull(serverSpan.get("resource_service.name"),
                    "Service name resource attribute should be set");
            System.out.println("Service name: " + serverSpan.get("resource_service.name"));

            assertNotNull(serverSpan.get("parentSpanId"), "Span should have parent span ID field");

        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

    protected void saveTaskInTaskStore(Task task) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task"))
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(task)))
                .header("Content-Type", APPLICATION_JSON)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Saving task failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
    }

    protected Task getTaskFromTaskStore(String taskId) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task/" + taskId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Getting task failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
        return JsonUtil.fromJson(response.body(), Task.class);
    }

    protected void deleteTaskInTaskStore(String taskId) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(("http://localhost:" + serverPort + "/test/task/" + taskId)))
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Deleting task failed!" + response.body());
        }
    }

    protected void ensureQueueForTask(String taskId) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/queue/ensure/" + taskId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Ensuring queue failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
    }
}
