package org.a2aproject.sdk.extras.opentelemetry.it;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.*;
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
    void testSendMessageStreamCreatesClosingSpan() throws Exception {
        reset();

        Client streamingClient = Client.builder(A2A.getAgentCard("http://localhost:" + serverPort))
                .clientConfig(new ClientConfig.Builder().setStreaming(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();

        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("stream test")))
                .messageId("stream-msg-1")
                .build();
        MessageSendParams params = new MessageSendParams(message, null, null, "");

        streamingClient.sendMessage(params, List.of(), null, null);

        await().atMost(10, SECONDS).until(() -> {
            List<Map<String, Object>> spans = getSpans();
            return spans.stream().anyMatch(s -> (A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-end").equals(s.get("name")));
        });

        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> initialSpan = spans.stream()
                .filter(s -> A2AMethods.SEND_STREAMING_MESSAGE_METHOD.equals(s.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No initial SendStreamingMessage span found"));

        Map<String, Object> closingSpan = spans.stream()
                .filter(s -> (A2AMethods.SEND_STREAMING_MESSAGE_METHOD + "-end").equals(s.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No SendStreamingMessage-stream closing span found"));

        assertEquals(Boolean.TRUE, initialSpan.get("ended"), "Initial span should be ended");
        assertEquals("SERVER", initialSpan.get("kind"), "Initial span should be SERVER");

        assertEquals(Boolean.TRUE, closingSpan.get("ended"), "Closing span should be ended");
        assertEquals("SERVER", closingSpan.get("kind"), "Closing span should be SERVER");

        assertTrue(((Number) closingSpan.get("events_count")).intValue() > 0,
                "Closing span should have at least one streaming event");
        assertEquals(1, ((Number) closingSpan.get("links_count")).intValue(),
                "Closing span should have exactly one link");
        assertEquals(initialSpan.get("spanId"), closingSpan.get("link_0_spanId"),
                "Closing span should link to the initial span");
    }

    @Test
    void testSendMessageStreamRecordsStreamingDuration() throws Exception {
        reset();
        given().get("/reset-metrics").then().statusCode(HTTP_OK);

        Client streamingClient = Client.builder(A2A.getAgentCard("http://localhost:" + serverPort))
                .clientConfig(new ClientConfig.Builder().setStreaming(true).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();

        Message message = Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("metric stream test")))
                .messageId("metric-stream-msg-1")
                .build();
        MessageSendParams params = new MessageSendParams(message, null, null, "");

        streamingClient.sendMessage(params, List.of(), null, null);

        await().atMost(10, SECONDS).until(() -> {
            List<Map<String, Object>> metrics = getMetrics();
            return metrics.stream().anyMatch(m -> "gen_ai.agent.a2a.streaming.duration".equals(m.get("name")));
        });

        List<Map<String, Object>> metrics = getMetrics();
        Map<String, Object> streamingMetric = metrics.stream()
                .filter(m -> "gen_ai.agent.a2a.streaming.duration".equals(m.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No streaming duration metric found"));

        assertEquals("HISTOGRAM", streamingMetric.get("type"),
                "Streaming duration metric should be a histogram");
        assertTrue(((Number) streamingMetric.get("data_count")).intValue() > 0,
                "Histogram should have at least one data point");
    }

    @Test
    void testPushNotificationDeliveryCreatesSpan() throws Exception {
        String taskId = "push-test-task-1";
        String contextId = "push-test-ctx-1";

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
            TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                    .id("push-test-config-1")
                    .taskId(taskId)
                    .url("http://localhost:" + serverPort + "/test/webhook")
                    .build();
            TaskPushNotificationConfig saved = client.createTaskPushNotificationConfiguration(config, null);
            assertNotNull(saved);

            reset();

            TaskStatusUpdateEvent event = new TaskStatusUpdateEvent(
                    taskId, new TaskStatus(TaskState.TASK_STATE_COMPLETED), contextId, null);
            enqueueTaskStatusUpdateEventViaHttp(taskId, event);

            await().atMost(15, SECONDS).until(() -> {
                List<Map<String, Object>> spans = getSpans();
                return spans.stream().anyMatch(s -> "SendPushNotification".equals(s.get("name")));
            });

            List<Map<String, Object>> spans = getSpans();
            Map<String, Object> pushSpan = spans.stream()
                    .filter(s -> "SendPushNotification".equals(s.get("name")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No SendPushNotification span found"));

            assertEquals(Boolean.TRUE, pushSpan.get("ended"), "Push notification span should be ended");
            assertEquals("CLIENT", pushSpan.get("kind"), "Push notification span should be CLIENT");
            assertEquals(taskId, pushSpan.get("attr_gen_ai.agent.a2a.task_id"),
                    "Task ID should be set on push notification span");
            assertNotNull(pushSpan.get("attr_gen_ai.agent.a2a.push_notification.event_kind"),
                    "Event kind should be set on push notification span");
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

    protected void enqueueTaskStatusUpdateEventViaHttp(String taskId, TaskStatusUpdateEvent event) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/queue/enqueueTaskStatusUpdateEvent/" + taskId))
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(event)))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Enqueue event failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
    }
}
