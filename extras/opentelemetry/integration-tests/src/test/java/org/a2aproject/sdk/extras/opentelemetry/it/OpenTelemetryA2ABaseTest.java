package org.a2aproject.sdk.extras.opentelemetry.it;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.GENAI_STREAMING_DURATION;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.STREAMING_END_SUFFIX;
import static org.a2aproject.sdk.extras.opentelemetry.A2AObservabilityNames.STREAMING_EVENT_SUFFIX;
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.a2aproject.sdk.spec.A2AMethods;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TextPart;

/**
 * Abstract base class for OpenTelemetry A2A integration tests.
 * Contains common test logic shared between unit and integration test modes.
 */
abstract class OpenTelemetryA2ABaseTest extends BaseTest {

    private static final Gson GSON = new Gson();
    private static final HttpClient TEST_HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    private static final String PUSH_TEST_TASK_ID = "push-propagation-task";
    private static final String PUSH_TEST_CONFIG_ID = "otel-push-config";

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
        given().delete("/test/dispatch-executor/count").then().statusCode(HTTP_OK);
        try {
            // Pre-test cleanup: remove any push config left behind by a previously failed test
            deletePushNotificationConfig(PUSH_TEST_TASK_ID, PUSH_TEST_CONFIG_ID);
        } catch (Exception ignored) {
        }
        clearExportedSpans();
    }

    protected void clearExportedSpans() {
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
        clearExportedSpans();

        try {
            Task retrievedTask = client.getTask(new TaskQueryParams(taskId), null);

            assertNotNull(retrievedTask);
            assertEquals(taskId, retrievedTask.id());

            await().atMost(10, SECONDS).until(() -> !getSpans().isEmpty());
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
        clearExportedSpans();

        ListTasksParams params = new ListTasksParams(
                null, null, null, null, null, null, null, ""
        );

        org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult result = client.listTasks(params, null);

        assertNotNull(result);

        await().atMost(10, SECONDS).until(() -> !getSpans().isEmpty());
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
        clearExportedSpans();

        try {
            Task cancelledTask = client.cancelTask(new CancelTaskParams(taskId), null);

            assertNotNull(cancelledTask);
            assertEquals(TaskState.TASK_STATE_CANCELED, cancelledTask.status().state());

            await().atMost(10, SECONDS).until(() -> !getSpans().isEmpty());
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
        clearExportedSpans();

        try {
            client.getTask(new TaskQueryParams(taskId), null);

            await().atMost(10, SECONDS).until(() -> !getSpans().isEmpty());
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
        given().get("/reset-metrics").then().statusCode(HTTP_OK);

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
            return spans.stream().anyMatch(s -> (A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX).equals(s.get("name")));
        });

        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> initialSpan = spans.stream()
                .filter(s -> A2AMethods.SEND_STREAMING_MESSAGE_METHOD.equals(s.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No initial SendStreamingMessage span found"));

        Map<String, Object> closingSpan = spans.stream()
                .filter(s -> (A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_END_SUFFIX).equals(s.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No SendStreamingMessage-stream closing span found"));

        assertEquals(Boolean.TRUE, initialSpan.get("ended"), "Initial span should be ended");
        assertEquals("SERVER", initialSpan.get("kind"), "Initial span should be SERVER");

        assertEquals(Boolean.TRUE, closingSpan.get("ended"), "Closing span should be ended");
        assertEquals("SERVER", closingSpan.get("kind"), "Closing span should be SERVER");

        assertTrue(
                spans.stream().anyMatch(s -> (A2AMethods.SEND_STREAMING_MESSAGE_METHOD + STREAMING_EVENT_SUFFIX).equals(s.get("name"))),
                "Should have at least one streaming event span");
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
            return metrics.stream().anyMatch(m -> GENAI_STREAMING_DURATION.equals(m.get("name")));
        });

        List<Map<String, Object>> metrics = getMetrics();
        Map<String, Object> streamingMetric = metrics.stream()
                .filter(m -> GENAI_STREAMING_DURATION.equals(m.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No streaming duration metric found"));

        assertEquals("HISTOGRAM", streamingMetric.get("type"),
                "Streaming duration metric should be a histogram");
        assertTrue(((Number) streamingMetric.get("data_count")).intValue() > 0,
                "Histogram should have at least one data point");
    }

    @Test
    void testPushNotificationCarriesTraceContext() throws Exception {
        String taskId = PUSH_TEST_TASK_ID;
        String contextId = "push-propagation-ctx";

        Task task = Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        clearExportedSpans();
        savePushNotificationConfig(taskId,
                TaskPushNotificationConfig.builder()
                        .id(PUSH_TEST_CONFIG_ID)
                        .url("http://localhost:" + serverPort + "/test/webhook")
                        .build());
        ensureQueueForTask(taskId);

        try {
            client.cancelTask(new CancelTaskParams(taskId), null);

            await().atMost(10, SECONDS).until(() -> !fetchWebhookRequests().isEmpty());

            Map<String, String> webhookHeaders = fetchWebhookRequests().get(0);
            String traceparent = assertTraceparentHeader(webhookHeaders);

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

            String traceIdFromHeader = traceparent.split("-")[1];
            assertEquals(traceIdFromHeader, pushSpan.get("traceId"),
                    "traceparent trace ID should match the SendPushNotification span's trace ID");
        } finally {
            deletePushNotificationConfig(taskId, PUSH_TEST_CONFIG_ID);
            deleteTaskInTaskStore(taskId);
        }
    }

    @Test
    void testPushNotificationPreservesCustomHttpClient() throws Exception {
        String taskId = "push-custom-client-task";
        String contextId = "push-custom-client-ctx";

        Task task = Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        clearExportedSpans();
        savePushNotificationConfig(taskId,
                TaskPushNotificationConfig.builder()
                        .id("custom-client-config")
                        .url("http://localhost:" + serverPort + "/test/webhook")
                        .build());
        ensureQueueForTask(taskId);

        try {
            client.cancelTask(new CancelTaskParams(taskId), null);

            await().atMost(10, SECONDS).until(() -> !fetchWebhookRequests().isEmpty());

            Map<String, String> webhookHeaders = fetchWebhookRequests().get(0);

            String customHeader = getHeader(webhookHeaders, CustomHttpClientProducer.CUSTOM_HEADER_NAME);
            assertNotNull(customHeader,
                    "Push notification must carry the custom client's marker header (" +
                    CustomHttpClientProducer.CUSTOM_HEADER_NAME + "), proving the OTel module " +
                    "wraps the application's HTTP client. Received headers: " + webhookHeaders.keySet());
            assertEquals(CustomHttpClientProducer.CUSTOM_HEADER_VALUE, customHeader);

            assertTraceparentHeader(webhookHeaders);
        } finally {
            deletePushNotificationConfig(taskId, "custom-client-config");
            deleteTaskInTaskStore(taskId);
        }
    }

    @Test
    void testPushNotificationUsesCustomDispatchExecutorWithTraceContext() throws Exception {
        String taskId = "push-custom-executor-task";
        String contextId = "push-custom-executor-ctx";

        Task task = Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(Collections.emptyList())
                .artifacts(Collections.emptyList())
                .build();

        saveTaskInTaskStore(task);
        clearExportedSpans();
        savePushNotificationConfig(taskId,
                TaskPushNotificationConfig.builder()
                        .id("custom-executor-config")
                        .url("http://localhost:" + serverPort + "/test/webhook")
                        .build());
        ensureQueueForTask(taskId);

        try {
            client.cancelTask(new CancelTaskParams(taskId), null);

            await().atMost(10, SECONDS).until(() -> !fetchWebhookRequests().isEmpty());

            Map<String, String> webhookHeaders = fetchWebhookRequests().get(0);
            assertTraceparentHeader(webhookHeaders);

            int executionCount = Integer.parseInt(
                    given().get("/test/dispatch-executor/count")
                            .then().statusCode(HTTP_OK)
                            .extract().body().asString());
            assertTrue(executionCount > 0,
                    "Custom @Named dispatch executor must be used by the OTel-wrapped executor, " +
                    "but execution count was 0");
        } finally {
            deletePushNotificationConfig(taskId, "custom-executor-config");
            deleteTaskInTaskStore(taskId);
        }
    }

    private static String getHeader(Map<String, String> headers, String name) {
        return headers.entrySet().stream()
                .filter(e -> name.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Asserts that the webhook headers contain a valid W3C {@code traceparent} header.
     *
     * @return the traceparent value for further assertions
     */
    private static String assertTraceparentHeader(Map<String, String> webhookHeaders) {
        String traceparent = getHeader(webhookHeaders, "traceparent");
        assertNotNull(traceparent,
                "Push notification should carry traceparent header. Received headers: " + webhookHeaders.keySet());
        assertTrue(traceparent.matches("00-[a-f0-9]{32}-[a-f0-9]{16}-[a-f0-9]{2}"),
                "traceparent should be in W3C format, got: " + traceparent);
        return traceparent;
    }

    private URI testUri(String path) {
        return URI.create("http://localhost:" + serverPort + "/test/" + path);
    }

    private HttpResponse<String> sendTestRequest(HttpRequest request) throws IOException, InterruptedException {
        return TEST_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void sendTestRequestExpecting200(HttpRequest request, String operation) throws IOException, InterruptedException {
        HttpResponse<String> response = sendTestRequest(request);
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(String.format("%s failed! Status: %d, Body: %s",
                    operation, response.statusCode(), response.body()));
        }
    }

    protected void saveTaskInTaskStore(Task task) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(testUri("task"))
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(task)))
                .header("Content-Type", "application/json")
                .build();
        sendTestRequestExpecting200(request, "Saving task");
    }

    protected Task getTaskFromTaskStore(String taskId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(testUri("task/" + taskId))
                .GET()
                .build();
        HttpResponse<String> response = sendTestRequest(request);
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException(String.format("Getting task failed! Status: %d, Body: %s",
                    response.statusCode(), response.body()));
        }
        return JsonUtil.fromJson(response.body(), Task.class);
    }

    protected void deleteTaskInTaskStore(String taskId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(testUri("task/" + taskId))
                .DELETE()
                .build();
        sendTestRequestExpecting200(request, "Deleting task");
    }

    protected void savePushNotificationConfig(String taskId, TaskPushNotificationConfig config) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(testUri("pushconfig/" + taskId))
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(config)))
                .header("Content-Type", "application/json")
                .build();
        sendTestRequestExpecting200(request, "Saving push config");
    }

    protected void deletePushNotificationConfig(String taskId, String configId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(testUri("pushconfig/" + taskId + "/" + configId))
                .DELETE()
                .build();
        sendTestRequestExpecting200(request, "Deleting push config");
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
        return GSON.fromJson(json, WEBHOOK_LIST_TYPE);
    }

    protected void ensureQueueForTask(String taskId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(testUri("queue/ensure/" + taskId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        sendTestRequestExpecting200(request, "Ensuring queue");
    }

}
