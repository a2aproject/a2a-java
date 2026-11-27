package org.a2aproject.sdk.client.transport.rest;

import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.CANCEL_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.GET_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.LIST_TASKS_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.SEND_MESSAGE_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
import static org.a2aproject.sdk.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.protobuf.Message;
import com.sun.net.httpserver.HttpServer;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientRoutingInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.grpc.CancelTaskRequest;
import org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.grpc.GetTaskRequest;
import org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.grpc.ListTasksRequest;
import org.a2aproject.sdk.grpc.SubscribeToTaskRequest;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.GetExtendedAgentCardParams;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RestInterceptorWireTest {
    private final LinkedBlockingQueue<WireRequest> requests = new LinkedBlockingQueue<>();
    private HttpServer server;
    private AgentInterface endpoint;
    private AgentCard card;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        endpoint = new AgentInterface("HTTP+JSON", "http://localhost:" + server.getAddress().getPort(), "defaulttenant");
        card = AgentCard.builder().name("test").description("test").version("1.0")
                .supportedInterfaces(List.of(endpoint)).capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of()).build();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            String method = exchange.getRequestMethod();
            requests.add(new WireRequest(method + " " + exchange.getRequestURI(),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    exchange.getRequestHeaders().getFirst("X-Probe")));
            String contentType = path.endsWith(":subscribe") || path.endsWith("/message:stream")
                    ? "text/event-stream" : "application/json";
            exchange.getResponseHeaders().add("Content-Type", contentType);
            byte[] body = responseBody(method, path).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void interceptedPostFieldsAgreeWithThePathAndHeaders(boolean immutable) throws Exception {
        try (var client = interceptedClient(immutable)) {
            client.sendMessage(message(), null);
            assertPost("/override/message:send", "override", Map.of());

            client.sendMessageStreaming(message(), event -> {}, error -> {}, null);
            assertPost("/override/message:stream", "override", Map.of());

            client.cancelTask(new CancelTaskParams("orig"), null);
            assertPost("/defaulttenant/tasks/changed:cancel", "defaulttenant", Map.of("id", "changed"));

            client.subscribeToTask(new TaskIdParams("orig"), event -> {}, error -> {}, null);
            assertPost("/defaulttenant/tasks/changed:subscribe", "defaulttenant", Map.of("id", "changed"));

            client.createTaskPushNotificationConfiguration(
                    TaskPushNotificationConfig.builder().taskId("orig").url("https://example.com").build(), null);
            assertPost("/defaulttenant/tasks/changed/pushNotificationConfigs", "defaulttenant", Map.of("taskId", "changed"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void interceptorReplacementsDriveGetAndDeleteUrls(boolean immutable) throws Exception {
        try (var client = interceptedClient(immutable)) {
            client.getExtendedAgentCard(new GetExtendedAgentCardParams(null), null);
            assertRequest("GET /override/extendedAgentCard", "yes");
            client.getTask(new TaskQueryParams("orig"), null);
            assertRequest("GET /" + (immutable ? "override" : "defaulttenant") + "/tasks/urn%3Atask%3Achanged?historyLength=0", "yes");
            client.listTasks(new ListTasksParams(), null);
            assertRequest("GET /defaulttenant/tasks?contextId=filtered&historyLength=0&includeArtifacts=false", "yes");
            client.getTaskPushNotificationConfiguration(new GetTaskPushNotificationConfigParams("orig", "config1"), null);
            assertRequest("GET /defaulttenant/tasks/changed/pushNotificationConfigs/config2", "yes");
            client.listTaskPushNotificationConfigurations(new ListTaskPushNotificationConfigsParams("orig"), null);
            assertRequest("GET /defaulttenant/tasks/changed/pushNotificationConfigs?pageSize=3", "yes");
            client.deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams("orig", "config1"), null);
            assertRequest("DELETE /defaulttenant/tasks/changed/pushNotificationConfigs/config2", "yes");
        }
    }

    @Test
    void omitsListTasksDefaultsAndClearedFields() throws Exception {
        var clear = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    @Nullable AgentCard agent, @Nullable ClientCallContext context) {
                if (payload instanceof GetTaskRequest.Builder builder) {
                    builder.clearHistoryLength();
                } else if (payload instanceof ListTasksRequest.Builder builder) {
                    builder.clearContextId().clearStatus().clearPageToken().clearHistoryLength().clearIncludeArtifacts();
                }
                return new PayloadAndHeaders(payload, headers);
            }
        };
        try (var client = new RestTransport(null, card, endpoint, List.of(clear))) {
            client.getTask(new TaskQueryParams("orig", 5), null);
            assertRequest("GET /defaulttenant/tasks/orig", null);
            client.listTasks(new ListTasksParams("original", TaskState.TASK_STATE_WORKING, null, "cursor", 5, null, true, null), null);
            assertRequest("GET /defaulttenant/tasks", null);
        }
        try (var client = new RestTransport(null, card, endpoint, null)) {
            client.listTasks(new ListTasksParams("", TaskState.TASK_STATE_UNSPECIFIED, null, "", null, null, null, null), null);
            assertRequest("GET /defaulttenant/tasks", null);
        }
    }

    @Test
    void rejectsNullInterceptorPayloads() throws Exception {
        var interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    @Nullable AgentCard agent, @Nullable ClientCallContext context) {
                return new PayloadAndHeaders(null, headers);
            }
        };
        try (var client = new RestTransport(null, card, endpoint, List.of(interceptor))) {
            assertThrows(IllegalArgumentException.class, () -> client.getTask(new TaskQueryParams("orig", 5), null));
            assertThrows(IllegalArgumentException.class,
                    () -> client.getExtendedAgentCard(new GetExtendedAgentCardParams(null), null));
            assertThrows(IllegalArgumentException.class, () -> client.sendMessage(message(), null));
        }
    }

    private RestTransport interceptedClient(boolean immutable) {
        var router = new ClientRoutingInterceptor() {
            @Override
            public @Nullable String routeTenant(@Nullable String destination, String method, @Nullable String tenant,
                    @Nullable AgentCard agent, @Nullable ClientCallContext context) {
                return switch (method) {
                    case SEND_MESSAGE_METHOD, SEND_STREAMING_MESSAGE_METHOD, GET_EXTENDED_AGENT_CARD_METHOD -> "override";
                    case GET_TASK_METHOD -> immutable ? "override" : tenant;
                    default -> tenant;
                };
            }
        };
        var interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    @Nullable AgentCard agent, @Nullable ClientCallContext context) {
                if (payload instanceof GetTaskRequest.Builder builder) {
                    builder.setId("urn:task:changed").setHistoryLength(0);
                } else if (payload instanceof CancelTaskRequest.Builder builder) {
                    builder.setId("changed");
                } else if (payload instanceof SubscribeToTaskRequest.Builder builder) {
                    builder.setId("changed");
                } else if (payload instanceof ListTasksRequest.Builder builder) {
                    builder.setContextId("filtered").setHistoryLength(0).setIncludeArtifacts(false);
                } else if (payload instanceof org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder) {
                    builder.setTaskId("changed");
                } else if (payload instanceof GetTaskPushNotificationConfigRequest.Builder builder) {
                    builder.setTaskId("changed").setId("config2");
                } else if (payload instanceof ListTaskPushNotificationConfigsRequest.Builder builder) {
                    builder.setTaskId("changed").setPageSize(3);
                } else if (payload instanceof DeleteTaskPushNotificationConfigRequest.Builder builder) {
                    builder.setTaskId("changed").setId("config2");
                }
                var updated = new HashMap<>(headers);
                updated.put("X-Probe", "yes");
                return new PayloadAndHeaders(immutable && payload instanceof Message.Builder builder ? builder.build() : payload,
                        updated);
            }
        };
        return new RestTransport(null, card, endpoint, List.of(interceptor, router));
    }

    private static MessageSendParams message() {
        var message = org.a2aproject.sdk.spec.Message.builder()
                .role(org.a2aproject.sdk.spec.Message.Role.ROLE_USER).messageId("m1").parts(List.of(new TextPart("hello")))
                .build();
        return MessageSendParams.builder().message(message).build();
    }

    private WireRequest assertRequest(String methodAndUri, @Nullable String marker) throws InterruptedException {
        WireRequest request = requests.poll(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals(methodAndUri, request.methodAndUri());
        assertEquals(marker, request.marker());
        return request;
    }

    private void assertPost(String path, String tenant, Map<String, String> fields) throws InterruptedException {
        var body = JsonParser.parseString(assertRequest("POST " + path, "yes").body()).getAsJsonObject();
        assertEquals(tenant, body.get("tenant").getAsString(), path);
        fields.forEach((key, value) -> assertEquals(value, body.get(key).getAsString(), path + " " + key));
    }

    private String responseBody(String method, String path) {
        if (path.endsWith(":subscribe")) {
            return TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
        }
        if (path.endsWith("/message:send")) {
            return SEND_MESSAGE_TEST_RESPONSE;
        }
        if (path.endsWith("/message:stream")) {
            return SEND_MESSAGE_STREAMING_TEST_RESPONSE;
        }
        if (path.endsWith("/extendedAgentCard")) {
            return new Gson().toJson(card);
        }
        if (method.equals("DELETE")) {
            return "";
        }
        if (path.endsWith("/pushNotificationConfigs")) {
            return method.equals("POST") ? SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE
                    : LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
        }
        if (path.contains("/pushNotificationConfigs/")) {
            return GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
        }
        if (path.endsWith(":cancel")) {
            return CANCEL_TASK_TEST_RESPONSE;
        }
        return path.endsWith("/tasks") ? LIST_TASKS_TEST_RESPONSE : GET_TASK_TEST_RESPONSE;
    }

    private record WireRequest(String methodAndUri, String body, @Nullable String marker) {
    }
}
