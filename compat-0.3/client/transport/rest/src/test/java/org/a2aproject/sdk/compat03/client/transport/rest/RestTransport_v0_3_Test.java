package org.a2aproject.sdk.compat03.client.transport.rest;


import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.CANCEL_TASK_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.CANCEL_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.GET_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_STREAMING_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.PayloadAndHeaders_v0_3;
import org.a2aproject.sdk.compat03.grpc.CancelTaskRequest;
import org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.GetAgentCardRequestOrBuilder;
import org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.GetTaskRequest;
import org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithUri_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3.Kind;
import org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class RestTransport_v0_3_Test {

    private static final Logger LOGGER = Logger.getLogger(RestTransport_v0_3_Test.class.getName());
    private static final Throwable NOT_INVOKED = new Throwable("terminal callback was not delivered");
    private ClientAndServer server;
    private static final AgentCard_v0_3 CARD = new AgentCard_v0_3.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .url("http://localhost:4001")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill_v0_3.Builder()
                                .id("hello_world")
                                .name("Returns hello world")
                                .description("just returns hello world")
                                .tags(Collections.singletonList("hello world"))
                                .examples(List.of("hi", "hello world"))
                                .build()))
                .protocolVersion("0.3.0")
                .build();

    @BeforeEach
    public void setUp() throws IOException {
        server = new ClientAndServer(4001);
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    public RestTransport_v0_3_Test() {
    }

    @Test
    public void getTaskUsesInterceptorResourceName() throws Exception {
        this.server.when(request().withMethod("GET").withPath("/v1/tasks/changed"))
                .respond(response().withStatusCode(200).withBody(GET_TASK_TEST_RESPONSE));
        ClientCallInterceptor_v0_3 interceptor = new ClientCallInterceptor_v0_3() {
            @Override
            public PayloadAndHeaders_v0_3 intercept(
                    String method, Object payload, Map<String, String> headers, AgentCard_v0_3 card,
                    ClientCallContext_v0_3 context) {
                ((GetTaskRequest.Builder) payload).setName("tasks/changed");
                return new PayloadAndHeaders_v0_3(payload, null);
            }
        };
        RestTransport_v0_3 client = new RestTransport_v0_3(null, CARD, CARD.url(), List.of(interceptor));
        client.getTask(new TaskQueryParams_v0_3("original"), null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void urlBoundFieldsFollowInterceptorBuilderAndImmutableMessages(boolean immutable) throws Exception {
        this.server.when(request().withMethod("GET").withPath("/v1/tasks/changed")
                .withQueryStringParameter("historyLength", "5"))
                .respond(response().withStatusCode(200).withBody(GET_TASK_TEST_RESPONSE));
        this.server.when(request().withMethod("POST").withPath("/v1/tasks/changed:cancel"))
                .respond(response().withStatusCode(200).withBody(CANCEL_TASK_TEST_RESPONSE));
        this.server.when(request().withMethod("POST").withPath("/v1/tasks/changed/pushNotificationConfigs")
                .withBody(JsonBody.json("""
                        {"parent":"tasks/changed","config":{"name":"tasks/changed/pushNotificationConfigs"}}
                        """, MatchType.ONLY_MATCHING_FIELDS)))
                .respond(response().withStatusCode(200).withBody(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE));
        this.server.when(request().withMethod("GET").withPath("/v1/tasks/changed/pushNotificationConfigs/other"))
                .respond(response().withStatusCode(200).withBody(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE));
        this.server.when(request().withMethod("GET").withPath("/v1/tasks/changed/pushNotificationConfigs"))
                .respond(response().withStatusCode(200).withBody(LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE));
        this.server.when(request().withMethod("DELETE").withPath("/v1/tasks/changed/pushNotificationConfigs/other"))
                .respond(response().withStatusCode(200));
        this.server.when(request().withMethod("POST").withPath("/v1/tasks/changed:subscribe"))
                .respond(response().withStatusCode(200).withHeader("Content-Type", "text/event-stream")
                        .withBody(TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE));
        ClientCallInterceptor_v0_3 interceptor = new ClientCallInterceptor_v0_3() {
            @Override
            public PayloadAndHeaders_v0_3 intercept(
                    String method, Object payload, Map<String, String> headers, AgentCard_v0_3 card,
                    ClientCallContext_v0_3 context) {
                Object result = payload;
                if (payload instanceof GetTaskRequest.Builder b) {
                    b.setName("tasks/changed").setHistoryLength(5); result = immutable ? b.build() : b;
                } else if (payload instanceof CancelTaskRequest.Builder b) {
                    b.setName("tasks/changed"); result = immutable ? b.build() : b;
                } else if (payload instanceof CreateTaskPushNotificationConfigRequest.Builder b) {
                    b.setParent("tasks/changed"); result = immutable ? b.build() : b;
                } else if (payload instanceof GetTaskPushNotificationConfigRequest.Builder b) {
                    assertEquals("tasks/original/pushNotificationConfigs/old", b.getName());
                    b.setName("tasks/changed/pushNotificationConfigs/other"); result = immutable ? b.build() : b;
                } else if (payload instanceof ListTaskPushNotificationConfigRequest.Builder b) {
                    b.setParent("tasks/changed").setPageSize(12).setPageToken("a b"); result = immutable ? b.build() : b;
                } else if (payload instanceof DeleteTaskPushNotificationConfigRequest.Builder b) {
                    assertEquals("tasks/original/pushNotificationConfigs/old", b.getName());
                    b.setName("tasks/changed/pushNotificationConfigs/other"); result = immutable ? b.build() : b;
                } else if (payload instanceof TaskSubscriptionRequest.Builder b) {
                    b.setName("tasks/changed"); result = immutable ? b.build() : b;
                }
                return new PayloadAndHeaders_v0_3(result, headers);
            }
        };
        RestTransport_v0_3 client = new RestTransport_v0_3(null, CARD, CARD.url(), List.of(interceptor));
        client.getTask(new TaskQueryParams_v0_3("original", 2), null);
        client.cancelTask(new TaskIdParams_v0_3("original"), null);
        client.setTaskPushNotificationConfiguration(new TaskPushNotificationConfig_v0_3("original",
                new PushNotificationConfig_v0_3.Builder().url("https://example.com/callback").build()), null);
        client.getTaskPushNotificationConfiguration(new GetTaskPushNotificationConfigParams_v0_3("original", "old", null), null);
        client.listTaskPushNotificationConfigurations(new ListTaskPushNotificationConfigParams_v0_3("original"), null);
        var lists = server.retrieveRecordedRequests(request().withMethod("GET")
                .withPath("/v1/tasks/changed/pushNotificationConfigs"));
        assertTrue(lists.length > 0);
        assertTrue(Arrays.stream(lists).allMatch(call -> call.getQueryStringParameters() == null
                || call.getQueryStringParameters().isEmpty()));
        client.deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams_v0_3("original", "old"), null);
        CountDownLatch latch = new CountDownLatch(1);
        client.resubscribe(new TaskIdParams_v0_3("original"), event -> latch.countDown(), error -> {}, null);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createPushConfigurationUsesInterceptedRawIdsInBodyAndEncodesUrl(boolean explicitConfigId) throws Exception {
        String taskId = "urn:task:café%2E";
        String configId = "cfg:jalapeño%2f";
        String parent = "tasks/" + taskId;
        var receivedUri = new AtomicReference<URI>();
        var receivedBody = new AtomicReference<String>();
        HttpServer capture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        capture.createContext("/", exchange -> {
            try (exchange) {
                receivedUri.set(exchange.getRequestURI());
                receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] responseBody = SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBody.length);
                exchange.getResponseBody().write(responseBody);
            }
        });
        capture.start();
        var interceptor = new ClientCallInterceptor_v0_3() {
            @Override
            public PayloadAndHeaders_v0_3 intercept(
                    String method, Object payload, Map<String, String> headers, AgentCard_v0_3 card,
                    ClientCallContext_v0_3 context) {
                var builder = (CreateTaskPushNotificationConfigRequest.Builder) payload;
                assertEquals("tasks/original/pushNotificationConfigs/old", builder.getConfig().getName());
                builder.setParent(parent);
                if (explicitConfigId) {
                    builder.setConfigId(configId);
                } else {
                    builder.clearConfigId();
                    builder.getConfigBuilder().setName("tasks/original/pushNotificationConfigs/" + configId);
                }
                return new PayloadAndHeaders_v0_3(builder.build(), headers);
            }
        };
        try (var client = new RestTransport_v0_3(null, CARD,
                "http://127.0.0.1:" + capture.getAddress().getPort(), List.of(interceptor))) {
            client.setTaskPushNotificationConfiguration(new TaskPushNotificationConfig_v0_3("original",
                    new PushNotificationConfig_v0_3.Builder().id("old").url("https://example.com/callback").build()), null);

            URI uri = receivedUri.get();
            assertEquals("/v1/tasks/urn%3Atask%3Acaf%C3%A9%252E/pushNotificationConfigs", uri.getRawPath());
            assertEquals("/v1/" + parent + "/pushNotificationConfigs", uri.getPath());
            var body = JsonParser.parseString(receivedBody.get()).getAsJsonObject();
            assertEquals(parent, body.get("parent").getAsString());
            assertEquals(parent + "/pushNotificationConfigs/" + configId,
                    body.getAsJsonObject("config").get("name").getAsString());
            if (explicitConfigId) {
                assertEquals(configId, body.get("configId").getAsString());
            } else {
                assertNull(body.get("configId"));
            }
        } finally {
            capture.stop(0);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("urlBoundOperations")
    void urlBoundOperationsRejectNullInterceptorPayloadBeforeSending(String operation,
            ThrowingConsumer<RestTransport_v0_3> call) {
        ClientCallInterceptor_v0_3 interceptor = new ClientCallInterceptor_v0_3() {
            @Override
            public PayloadAndHeaders_v0_3 intercept(
                    String method, Object payload, Map<String, String> headers, AgentCard_v0_3 card,
                    ClientCallContext_v0_3 context) {
                return new PayloadAndHeaders_v0_3(null, null);
            }
        };
        try (var client = new RestTransport_v0_3(null, CARD, CARD.url(), List.of(interceptor))) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> call.accept(client));
            assertTrue(error.getMessage().contains("payload"));
            assertEquals(0, server.retrieveRecordedRequests(request()).length);
        }
    }

    private static Stream<Arguments> urlBoundOperations() {
        return Stream.of(
                operation("getTask", client -> client.getTask(new TaskQueryParams_v0_3("original", 2), null)),
                operation("cancelTask", client -> client.cancelTask(new TaskIdParams_v0_3("original"), null)),
                operation("createPushConfig", client -> client.setTaskPushNotificationConfiguration(
                        new TaskPushNotificationConfig_v0_3("original",
                                new PushNotificationConfig_v0_3.Builder().url("https://example.com/callback").build()), null)),
                operation("getPushConfig", client -> client.getTaskPushNotificationConfiguration(
                        new GetTaskPushNotificationConfigParams_v0_3("original", "old", null), null)),
                operation("listPushConfigs", client -> client.listTaskPushNotificationConfigurations(
                        new ListTaskPushNotificationConfigParams_v0_3("original"), null)),
                operation("deletePushConfig", client -> client.deleteTaskPushNotificationConfigurations(
                        new DeleteTaskPushNotificationConfigParams_v0_3("original", "old"), null)),
                operation("resubscribe", client -> client.resubscribe(new TaskIdParams_v0_3("original"),
                        event -> {}, error -> {}, null)));
    }

    private static Arguments operation(String name, ThrowingConsumer<RestTransport_v0_3> call) {
        return Arguments.of(name, call);
    }

    @Test
    public void authenticatedExtendedCardPassesRequestAndAllowsNullHeaders() throws Exception {
        server.when(request().withMethod("GET").withPath("/.well-known/agent-card.json"))
                .respond(response().withStatusCode(200).withBody(JsonRestMessages_v0_3.AGENT_CARD_SUPPORTS_EXTENDED));
        server.when(request().withMethod("GET").withPath("/v1/card"))
                .respond(response().withStatusCode(200).withBody(JsonRestMessages_v0_3.AUTHENTICATION_EXTENDED_AGENT_CARD));
        AtomicInteger calls = new AtomicInteger();
        ClientCallInterceptor_v0_3 interceptor = new ClientCallInterceptor_v0_3() {
            @Override
            public PayloadAndHeaders_v0_3 intercept(
                    String method, Object payload, Map<String, String> headers, AgentCard_v0_3 card,
                    ClientCallContext_v0_3 context) {
                assertTrue(payload instanceof GetAgentCardRequestOrBuilder);
                calls.incrementAndGet();
                return new PayloadAndHeaders_v0_3(payload, null);
            }
        };
        RestTransport_v0_3 client = new RestTransport_v0_3(null, null, CARD.url(), List.of(interceptor));
        assertEquals("GeoSpatial Route Planner Agent Extended", client.getAgentCard(null).name());
        assertEquals(1, calls.get());
    }

    /**
     * Test of sendMessage method, of class JSONRestTransport.
     */
    @Test
    public void testSendMessage() throws Exception {
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .taskId("")
                .build();
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:send")
                        .withBody(JsonBody.json(SEND_MESSAGE_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_TEST_RESPONSE)
                );
        MessageSendParams_v0_3 messageSendParams = new MessageSendParams_v0_3(message, null, null);
        ClientCallContext_v0_3 context = null;
        
        RestTransport_v0_3 instance = new RestTransport_v0_3(CARD);
        EventKind_v0_3 result = instance.sendMessage(messageSendParams, context);
        assertEquals("task", result.kind());
        Task_v0_3 task = (Task_v0_3) result;
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", task.id());
        assertEquals("context-1234", task.contextId());
        assertEquals(TaskState_v0_3.SUBMITTED, task.status().state());
        assertNull(task.status().message());
        assertNull(task.metadata());
        assertEquals(true, task.artifacts().isEmpty());
        assertEquals(1, task.history().size());
        Message_v0_3 history = task.history().get(0);
        assertEquals("message", history.kind());
        assertEquals(Message_v0_3.Role.USER, history.role());
        assertEquals("context-1234", history.contextId());
        assertEquals("message-1234", history.messageId());
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", history.taskId());
        assertEquals(1, history.parts().size());
        assertEquals(Kind.TEXT, ((TextPart_v0_3) history.parts().get(0)).kind());
        assertEquals("tell me a joke", ((TextPart_v0_3) history.parts().get(0)).text());
        assertNull(history.metadata());
        assertNull(history.referenceTaskIds());
    }

    /**
     * Test of cancelTask method, of class JSONRestTransport.
     */
    @Test
    public void testCancelTask() throws Exception {
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64:cancel")
                        .withBody(JsonBody.json(CANCEL_TASK_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(CANCEL_TASK_TEST_RESPONSE)
                );
        ClientCallContext_v0_3 context = null;
        RestTransport_v0_3 instance = new RestTransport_v0_3(CARD);
        Task_v0_3 task = instance.cancelTask(new TaskIdParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64",
                new HashMap<>()), context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertEquals(TaskState_v0_3.CANCELED, task.status().state());
        assertNull(task.status().message());
        assertNull(task.metadata());
    }

    /**
     * Test of getTask method, of class JSONRestTransport.
     */
    @Test
    public void testGetTask() throws Exception {
        this.server.when(
                request()
                        .withMethod("GET")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64")
                        .withQueryStringParameter("historyLength", "5")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_TEST_RESPONSE)
                );
        ClientCallContext_v0_3 context = null;
        TaskQueryParams_v0_3 request = new TaskQueryParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64", 5);
        RestTransport_v0_3 instance = new RestTransport_v0_3(CARD);
        Task_v0_3 task = instance.getTask(request, context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.id());
        assertEquals(TaskState_v0_3.COMPLETED, task.status().state());
        assertNull(task.status().message());
        assertNull(task.metadata());
        assertEquals(false, task.artifacts().isEmpty());
        assertEquals(1, task.artifacts().size());
        Artifact_v0_3 artifact = task.artifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("", artifact.name());
        assertEquals(false, artifact.parts().isEmpty());
        assertEquals(Kind.TEXT, ((TextPart_v0_3) artifact.parts().get(0)).kind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) artifact.parts().get(0)).text());
        assertEquals(1, task.history().size());
        Message_v0_3 history = task.history().get(0);
        assertEquals("message", history.kind());
        assertEquals(Message_v0_3.Role.USER, history.role());
        assertEquals("message-123", history.messageId());
        assertEquals(3, history.parts().size());
        assertEquals(Kind.TEXT, ((TextPart_v0_3) history.parts().get(0)).kind());
        assertEquals("tell me a joke", ((TextPart_v0_3) history.parts().get(0)).text());
        assertEquals(Kind.FILE, ((FilePart_v0_3) history.parts().get(1)).kind());
        FilePart_v0_3 part = (FilePart_v0_3) history.parts().get(1);
        assertEquals("text/plain", part.file().mimeType());
        assertEquals("file.txt", part.file().name());
        assertEquals("file:///path/to/file.txt", ((FileWithUri_v0_3) part.file()).uri());
        part = (FilePart_v0_3) history.parts().get(2);
        assertEquals(Kind.FILE, part.kind());
        assertEquals("text/plain", part.file().mimeType());
        assertEquals("greeting.txt", part.file().name());
        assertEquals("hello", ((FileWithBytes_v0_3) part.file()).bytes());
        assertNull(history.metadata());
        assertNull(history.referenceTaskIds());
    }

    /**
     * Test of sendMessageStreaming method, of class JSONRestTransport.
     */
    @Test
    public void testSendMessageStreaming() throws Exception {
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:stream")
                        .withBody(JsonBody.json(SEND_MESSAGE_STREAMING_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(SEND_MESSAGE_STREAMING_TEST_RESPONSE)
                );

        RestTransport_v0_3 client = new RestTransport_v0_3(CARD);
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("tell me some jokes")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();
        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<StreamingEventKind_v0_3> eventHandler = event -> {
            receivedEvent.set(event);
            latch.countDown();
        };
        Consumer<Throwable> errorHandler = error -> {
        };
        client.sendMessageStreaming(params, eventHandler, errorHandler, null);

        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(eventReceived);
        assertNotNull(receivedEvent.get());
        assertEquals("task", receivedEvent.get().kind());
        Task_v0_3 task = (Task_v0_3) receivedEvent.get();
        assertEquals("2", task.id());
    }

    /**
     * Test of setTaskPushNotificationConfiguration method, of class JSONRestTransport.
     */
    @Test
    public void testSetTaskPushNotificationConfiguration() throws Exception {
        LOGGER.info("Testing setTaskPushNotificationConfiguration");
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs")
                        .withBody(JsonBody.json(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE)
                );
        RestTransport_v0_3 client = new RestTransport_v0_3(CARD);
        TaskPushNotificationConfig_v0_3 pushedConfig = new TaskPushNotificationConfig_v0_3(
                "de38c76d-d54c-436c-8b9f-4c2703648d64",
                new PushNotificationConfig_v0_3.Builder()
                        .url("https://example.com/callback")
                        .authenticationInfo(
                                new PushNotificationAuthenticationInfo_v0_3(Collections.singletonList("jwt"), null))
                        .build());
        TaskPushNotificationConfig_v0_3 taskPushNotificationConfig = client.setTaskPushNotificationConfiguration(pushedConfig, null);
        PushNotificationConfig_v0_3 pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo_v0_3 authenticationInfo = pushNotificationConfig.authentication();
        assertEquals(1, authenticationInfo.schemes().size());
        assertEquals("jwt", authenticationInfo.schemes().get(0));
    }

    /**
     * Test of getTaskPushNotificationConfiguration method, of class JSONRestTransport.
     */
    @Test
    public void testGetTaskPushNotificationConfiguration() throws Exception {
        this.server.when(
                request()
                        .withMethod("GET")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/10")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE)
                );

        RestTransport_v0_3 client = new RestTransport_v0_3(CARD);
        TaskPushNotificationConfig_v0_3 taskPushNotificationConfig = client.getTaskPushNotificationConfiguration(
                new GetTaskPushNotificationConfigParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64", "10",
                        new HashMap<>()), null);
        PushNotificationConfig_v0_3 pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        PushNotificationAuthenticationInfo_v0_3 authenticationInfo = pushNotificationConfig.authentication();
        assertTrue(authenticationInfo.schemes().size() == 1);
        assertEquals("jwt", authenticationInfo.schemes().get(0));
    }

    /**
     * Test of listTaskPushNotificationConfigurations method, of class JSONRestTransport.
     */
    @Test
    public void testListTaskPushNotificationConfigurations() throws Exception {
        this.server.when(
                request()
                        .withMethod("GET")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE)
                );

        RestTransport_v0_3 client = new RestTransport_v0_3(CARD);
        List<TaskPushNotificationConfig_v0_3> taskPushNotificationConfigs = client.listTaskPushNotificationConfigurations(
                new ListTaskPushNotificationConfigParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64", new HashMap<>()), null);
        assertEquals(2, taskPushNotificationConfigs.size());
        PushNotificationConfig_v0_3 pushNotificationConfig = taskPushNotificationConfigs.get(0).pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        assertEquals("10", pushNotificationConfig.id());
        PushNotificationAuthenticationInfo_v0_3 authenticationInfo = pushNotificationConfig.authentication();
        assertTrue(authenticationInfo.schemes().size() == 1);
        assertEquals("jwt", authenticationInfo.schemes().get(0));
        assertEquals("", authenticationInfo.credentials());
        pushNotificationConfig = taskPushNotificationConfigs.get(1).pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://test.com/callback", pushNotificationConfig.url());
        assertEquals("5", pushNotificationConfig.id());
        authenticationInfo = pushNotificationConfig.authentication();
        assertNull(authenticationInfo);
    }

    /**
     * Test of deleteTaskPushNotificationConfigurations method, of class JSONRestTransport.
     */
    @Test
    public void testDeleteTaskPushNotificationConfigurations() throws Exception {
        LOGGER.info("Testing deleteTaskPushNotificationConfigurations");
        this.server.when(
                request()
                        .withMethod("DELETE")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/10")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                );
        ClientCallContext_v0_3 context = null;
        RestTransport_v0_3 instance = new RestTransport_v0_3(CARD);
        instance.deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64", "10"), context);
    }

    /**
     * Test of resubscribe method, of class JSONRestTransport.
     */
    @Test
    public void testResubscribe() throws Exception {
        LOGGER.info("Testing resubscribe");
        
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/v1/tasks/task-1234:subscribe")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE)
                );

        RestTransport_v0_3 client = new RestTransport_v0_3(CARD);
        TaskIdParams_v0_3 taskIdParams = new TaskIdParams_v0_3("task-1234");

        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<StreamingEventKind_v0_3> eventHandler = event -> {
            receivedEvent.set(event);
            latch.countDown();
        };
        Consumer<Throwable> errorHandler = error -> {};
        client.resubscribe(taskIdParams, eventHandler, errorHandler, null);

        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(eventReceived);

        StreamingEventKind_v0_3 eventKind = receivedEvent.get();;
        assertNotNull(eventKind);
        assertInstanceOf(Task_v0_3.class, eventKind);
        Task_v0_3 task = (Task_v0_3) eventKind;
        assertEquals("2", task.id());
        assertEquals("context-1234", task.contextId());
        assertEquals(TaskState_v0_3.COMPLETED, task.status().state());
        List<Artifact_v0_3> artifacts = task.artifacts();
        assertEquals(1, artifacts.size());
        Artifact_v0_3 artifact = artifacts.get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        Part_v0_3<?> part = artifact.parts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, ((TextPart_v0_3) part).kind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) part).text());
    }

    @Test
    public void testGetTaskPreservesDataAndMetadataNulls() throws Exception {
        String json = """
                {"id":"task-null","contextId":"context-null","status":{"state":"TASK_STATE_COMPLETED"},
                   "history":[{"messageId":"message-null","role":"ROLE_AGENT",
                     "content":[{"data":{"data":{"optional":null,"nested":{},"items":[null,{"optional":null},{},[]]}},
                       "metadata":{"optional":null}}],"metadata":{"optional":null}}],"metadata":{"optional":null}}
                """;
        server.when(request().withMethod("GET").withPath("/v1/tasks/task-null"))
                .respond(response().withStatusCode(200).withBody(json));

        var task = new RestTransport_v0_3(CARD).getTask(new TaskQueryParams_v0_3("task-null", 0), null);
        Map<String, Object> nullField = Collections.singletonMap("optional", null);
        assertEquals(nullField, task.metadata());
        assertEquals(nullField, task.history().get(0).metadata());
        var part = (DataPart_v0_3) task.history().get(0).parts().get(0);
        assertEquals(nullField, part.metadata());
        assertTrue(part.data().containsKey("optional"));
        assertNull(part.data().get("optional"));
        assertEquals(Map.of(), part.data().get("nested"));
        assertEquals(Arrays.asList(null, nullField, Map.of(), List.of()), part.data().get("items"));
    }

    /**
     * A stream that ends normally has to be reported through the error consumer with a null
     * throwable, the same way the native REST transport and the 0.3 JSON-RPC transport already do
     * it. Without that signal a caller cannot tell a finished stream from a still running one.
     */
    @Test
    public void testSendMessageStreamingSignalsNormalCompletion() throws Exception {
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:stream")
                        .withBody(JsonBody.json(SEND_MESSAGE_STREAMING_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(SEND_MESSAGE_STREAMING_TEST_RESPONSE)
                );

        RestTransport_v0_3 client = new RestTransport_v0_3(CARD);
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("tell me some jokes")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        CountDownLatch eventLatch = new CountDownLatch(1);
        CountDownLatch terminalLatch = new CountDownLatch(1);
        AtomicInteger terminalCalls = new AtomicInteger();
        AtomicReference<Throwable> terminalError = new AtomicReference<>(NOT_INVOKED);
        Consumer<StreamingEventKind_v0_3> eventHandler = event -> eventLatch.countDown();
        Consumer<Throwable> errorHandler = error -> {
            terminalError.set(error);
            terminalCalls.incrementAndGet();
            terminalLatch.countDown();
        };
        client.sendMessageStreaming(params, eventHandler, errorHandler, null);

        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Should receive streaming event");
        assertTrue(terminalLatch.await(10, TimeUnit.SECONDS), "Error consumer should be called when the stream ends");
        assertNull(terminalError.get(), "Normal completion has to be reported as a null error");
        assertEquals(1, terminalCalls.get(), "A single stream has to deliver exactly one terminal callback");
    }

    /**
     * Same contract as above for a resubscription stream.
     */
    @Test
    public void testResubscribeSignalsNormalCompletion() throws Exception {
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/tasks/task-1234:subscribe")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE)
                );

        RestTransport_v0_3 client = new RestTransport_v0_3(CARD);
        TaskIdParams_v0_3 taskIdParams = new TaskIdParams_v0_3("task-1234");

        CountDownLatch eventLatch = new CountDownLatch(1);
        CountDownLatch terminalLatch = new CountDownLatch(1);
        AtomicInteger terminalCalls = new AtomicInteger();
        AtomicReference<Throwable> terminalError = new AtomicReference<>(NOT_INVOKED);
        Consumer<StreamingEventKind_v0_3> eventHandler = event -> eventLatch.countDown();
        Consumer<Throwable> errorHandler = error -> {
            terminalError.set(error);
            terminalCalls.incrementAndGet();
            terminalLatch.countDown();
        };
        client.resubscribe(taskIdParams, eventHandler, errorHandler, null);

        assertTrue(eventLatch.await(10, TimeUnit.SECONDS), "Should receive streaming event");
        assertTrue(terminalLatch.await(10, TimeUnit.SECONDS), "Error consumer should be called when the stream ends");
        assertNull(terminalError.get(), "Normal completion has to be reported as a null error");
        assertEquals(1, terminalCalls.get(), "A single stream has to deliver exactly one terminal callback");
    }
}
