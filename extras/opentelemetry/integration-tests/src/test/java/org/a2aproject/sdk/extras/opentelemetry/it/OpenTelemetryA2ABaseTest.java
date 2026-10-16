package org.a2aproject.sdk.extras.opentelemetry.it;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import io.opentelemetry.api.trace.SpanKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
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
        given().delete("/test/webhook/requests").then().statusCode(HTTP_OK);
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
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
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

        org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult result = client.listTasks(params, null);

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
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        ensureQueueForTask(taskId);
        reset();

        try {
            Task cancelledTask = client.cancelTask(new CancelTaskParams(taskId), null);

            assertNotNull(cancelledTask);
            assertEquals(TaskState.TASK_STATE_CANCELED, cancelledTask.status().state());

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
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
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

    @Test
    void testPushNotificationCarriesTraceContext() throws Exception {
        String taskId = "push-propagation-task";
        String contextId = "push-propagation-ctx";

        Task task = Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        reset();
        savePushNotificationConfig(taskId,
                TaskPushNotificationConfig.builder()
                        .id("otel-push-config")
                        .url("http://localhost:" + serverPort + "/test/webhook")
                        .build());
        ensureQueueForTask(taskId);

        try {
            // Cancel the task to trigger push notification delivery
            client.cancelTask(new CancelTaskParams(taskId), null);

            // Wait for the webhook to receive the push notification (queried via HTTP
            // so this works in both @QuarkusTest and @QuarkusIntegrationTest modes)
            await().atMost(10, SECONDS).until(() -> !fetchWebhookRequests().isEmpty());

            // Verify traceparent header is present
            Map<String, String> webhookHeaders = fetchWebhookRequests().get(0);
            String traceparent = webhookHeaders.entrySet().stream()
                    .filter(e -> "traceparent".equalsIgnoreCase(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

            assertNotNull(traceparent,
                    "Push notification should carry traceparent header. Received headers: " + webhookHeaders.keySet());

            // Verify traceparent format: 00-<32-hex-trace-id>-<16-hex-span-id>-<2-hex-flags>
            assertTrue(traceparent.matches("00-[a-f0-9]{32}-[a-f0-9]{16}-[a-f0-9]{2}"),
                    "traceparent should be in W3C format, got: " + traceparent);

            // Wait for the SendPushNotification CLIENT span to be exported
            await().atMost(10, SECONDS).until(() -> getSpans().stream()
                    .anyMatch(span -> "SendPushNotification".equals(span.get("name"))
                            && SpanKind.CLIENT.name().equals(span.get("kind"))));

            List<Map<String, Object>> spans = getSpans();
            Map<String, Object> pushSpan = spans.stream()
                    .filter(span -> "SendPushNotification".equals(span.get("name")))
                    .filter(span -> SpanKind.CLIENT.name().equals(span.get("kind")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No CLIENT span found for SendPushNotification. Spans: "
                            + spans.stream().map(s -> s.get("name")).toList()));

            assertNotNull(pushSpan.get("spanId"));
            assertNotNull(pushSpan.get("traceId"));

            // The traceparent's trace-id should match the SendPushNotification span's trace ID
            String traceIdFromHeader = traceparent.split("-")[1];
            assertEquals(traceIdFromHeader, pushSpan.get("traceId"),
                    "traceparent trace ID should match the SendPushNotification span's trace ID");
        } finally {
            deletePushNotificationConfig(taskId, "otel-push-config");
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
                .header("Content-Type", "application/json")
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

    protected void savePushNotificationConfig(String taskId, TaskPushNotificationConfig config) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/pushconfig/" + taskId))
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(config)))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Saving push config failed! Status: %d, Body: %s",
                    response.statusCode(), response.body()));
        }
    }

    protected void deletePushNotificationConfig(String taskId, String configId) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/pushconfig/" + taskId + "/" + configId))
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Deleting push config failed! Status: %d, Body: %s",
                    response.statusCode(), response.body()));
        }
    }

    private static final Type WEBHOOK_LIST_TYPE = new TypeToken<List<Map<String, String>>>() {}.getType();

    /**
     * Fetches captured webhook requests via HTTP, so it works across JVM
     * boundaries in {@code @QuarkusIntegrationTest} mode.
     */
    protected List<Map<String, String>> fetchWebhookRequests() {
        String json = given().get("/test/webhook/requests")
                .then().statusCode(HTTP_OK)
                .extract().body().asString();
        return new Gson().fromJson(json, WEBHOOK_LIST_TYPE);
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
