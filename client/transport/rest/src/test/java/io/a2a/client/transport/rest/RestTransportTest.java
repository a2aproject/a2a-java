package io.a2a.client.transport.rest;


import static io.a2a.client.transport.rest.JsonRestMessages.CANCEL_TASK_TEST_REQUEST;
import static io.a2a.client.transport.rest.JsonRestMessages.CANCEL_TASK_TEST_RESPONSE;
import static io.a2a.client.transport.rest.JsonRestMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static io.a2a.client.transport.rest.JsonRestMessages.GET_TASK_TEST_RESPONSE;
import static io.a2a.client.transport.rest.JsonRestMessages.LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static io.a2a.client.transport.rest.JsonRestMessages.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static io.a2a.client.transport.rest.JsonRestMessages.SEND_MESSAGE_TEST_REQUEST;
import static io.a2a.client.transport.rest.JsonRestMessages.SEND_MESSAGE_TEST_RESPONSE;
import static io.a2a.client.transport.rest.JsonRestMessages.SEND_MESSAGE_STREAMING_TEST_REQUEST;
import static io.a2a.client.transport.rest.JsonRestMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static io.a2a.client.transport.rest.JsonRestMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static io.a2a.client.transport.rest.JsonRestMessages.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.Artifact;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.Part.Kind;
import io.a2a.spec.AuthenticationInfo;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class RestTransportTest {

    private static final Logger log = Logger.getLogger(RestTransportTest.class.getName());
    private ClientAndServer server;
    private static final AgentCard CARD = new AgentCard.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .supportedInterfaces(Collections.singletonList(new io.a2a.spec.AgentInterface("HTTP+JSON", "http://localhost:4001")))
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
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

    public RestTransportTest() {
    }

    /**
     * Test of sendMessage method, of class JSONRestTransport.
     */
    @Test
    public void testSendMessage() throws Exception {
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
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
        MessageSendParams messageSendParams = new MessageSendParams(message, null, null, "");
        ClientCallContext context = null;
        
        RestTransport instance = new RestTransport(CARD);
        EventKind result = instance.sendMessage(messageSendParams, context);
        assertEquals("task", result.getKind());
        Task task = (Task) result;
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", task.getId());
        assertEquals("context-1234", task.getContextId());
        assertEquals(TaskState.SUBMITTED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNull(task.getMetadata());
        assertEquals(true, task.getArtifacts().isEmpty());
        assertEquals(1, task.getHistory().size());
        Message history = task.getHistory().get(0);
        assertEquals("message", history.getKind());
        assertEquals(Message.Role.USER, history.getRole());
        assertEquals("context-1234", history.getContextId());
        assertEquals("message-1234", history.getMessageId());
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", history.getTaskId());
        assertEquals(1, history.getParts().size());
        assertEquals(Kind.TEXT, history.getParts().get(0).getKind());
        assertEquals("tell me a joke", ((TextPart) history.getParts().get(0)).getText());
        assertNull(task.getMetadata());
        assertNull(history.getReferenceTaskIds());
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
        ClientCallContext context = null;
        RestTransport instance = new RestTransport(CARD);
        Task task = instance.cancelTask(new TaskIdParams("de38c76d-d54c-436c-8b9f-4c2703648d64"), context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals(TaskState.CANCELED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNotNull(task.getMetadata());
        assertTrue(task.getMetadata().isEmpty());
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
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_TEST_RESPONSE)
                );
        ClientCallContext context = null;
        TaskQueryParams request = new TaskQueryParams("de38c76d-d54c-436c-8b9f-4c2703648d64", 10);
        RestTransport instance = new RestTransport(CARD);
        Task task = instance.getTask(request, context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNotNull(task.getMetadata());
        assertTrue(task.getMetadata().isEmpty());
        assertEquals(false, task.getArtifacts().isEmpty());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertNull(artifact.name());
        assertEquals(false, artifact.parts().isEmpty());
        assertEquals(Kind.TEXT, artifact.parts().get(0).getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) artifact.parts().get(0)).getText());
        assertEquals(1, task.getHistory().size());
        Message history = task.getHistory().get(0);
        assertEquals("message", history.getKind());
        assertEquals(Message.Role.USER, history.getRole());
        assertEquals("message-123", history.getMessageId());
        assertEquals(3, history.getParts().size());
        assertEquals(Kind.TEXT, history.getParts().get(0).getKind());
        assertEquals("tell me a joke", ((TextPart) history.getParts().get(0)).getText());
        assertEquals(Kind.FILE, history.getParts().get(1).getKind());
        FilePart part = (FilePart) history.getParts().get(1);
        assertEquals("text/plain", part.getFile().mimeType());
        assertEquals("file:///path/to/file.txt", ((FileWithUri) part.getFile()).uri());
        part = (FilePart) history.getParts().get(2);
        assertEquals(Kind.FILE, part.getKind());
        assertEquals("text/plain", part.getFile().mimeType());
        assertEquals("aGVsbG8=", ((FileWithBytes) part.getFile()).bytes());
        assertNull(history.getMetadata());
        assertNull(history.getReferenceTaskIds());
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

        RestTransport client = new RestTransport(CARD);
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me some jokes")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .configuration(configuration)
                .build();
        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<StreamingEventKind> eventHandler = event -> {
            receivedEvent.set(event);
            latch.countDown();
        };
        Consumer<Throwable> errorHandler = error -> {
        };
        client.sendMessageStreaming(params, eventHandler, errorHandler, null);

        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(eventReceived);
        assertNotNull(receivedEvent.get());
        assertEquals("task", receivedEvent.get().getKind());
        Task task = (Task) receivedEvent.get();
        assertEquals("2", task.getId());
    }

    /**
     * Test of setTaskPushNotificationConfiguration method, of class JSONRestTransport.
     */
    @Test
    public void testSetTaskPushNotificationConfiguration() throws Exception {
        log.info("Testing setTaskPushNotificationConfiguration");
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
        RestTransport client = new RestTransport(CARD);
        TaskPushNotificationConfig pushedConfig = new TaskPushNotificationConfig(
                "de38c76d-d54c-436c-8b9f-4c2703648d64",
                new PushNotificationConfig.Builder()
                        .id("default-config-id")
                        .url("https://example.com/callback")
                        .authenticationInfo(
                                new AuthenticationInfo(Collections.singletonList("jwt"), null))
                        .build(), "tenant");
        TaskPushNotificationConfig taskPushNotificationConfig = client.setTaskPushNotificationConfiguration(pushedConfig, null);
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        AuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
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

        RestTransport client = new RestTransport(CARD);
        TaskPushNotificationConfig taskPushNotificationConfig = client.getTaskPushNotificationConfiguration(
                new GetTaskPushNotificationConfigParams("de38c76d-d54c-436c-8b9f-4c2703648d64", "10"), null);
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfig.pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        AuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
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

        RestTransport client = new RestTransport(CARD);
        List<TaskPushNotificationConfig> taskPushNotificationConfigs = client.listTaskPushNotificationConfigurations(
                new ListTaskPushNotificationConfigParams("de38c76d-d54c-436c-8b9f-4c2703648d64"), null);
        assertEquals(2, taskPushNotificationConfigs.size());
        PushNotificationConfig pushNotificationConfig = taskPushNotificationConfigs.get(0).pushNotificationConfig();
        assertNotNull(pushNotificationConfig);
        assertEquals("https://example.com/callback", pushNotificationConfig.url());
        assertEquals("10", pushNotificationConfig.id());
        AuthenticationInfo authenticationInfo = pushNotificationConfig.authentication();
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
        log.info("Testing deleteTaskPushNotificationConfigurations");
        this.server.when(
                request()
                        .withMethod("DELETE")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/10")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                );
        ClientCallContext context = null;
        RestTransport instance = new RestTransport(CARD);
        instance.deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams("de38c76d-d54c-436c-8b9f-4c2703648d64", "10"), context);
    }

    /**
     * Test of resubscribe method, of class JSONRestTransport.
     */
    @Test
    public void testResubscribe() throws Exception {
        log.info("Testing resubscribe");
        
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

        RestTransport client = new RestTransport(CARD);
        TaskIdParams taskIdParams = new TaskIdParams("task-1234");

        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<StreamingEventKind> eventHandler = event -> {
            receivedEvent.set(event);
            latch.countDown();
        };
        Consumer<Throwable> errorHandler = error -> {};
        client.resubscribe(taskIdParams, eventHandler, errorHandler, null);

        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(eventReceived);

        StreamingEventKind eventKind = receivedEvent.get();;
        assertNotNull(eventKind);
        assertInstanceOf(Task.class, eventKind);
        Task task = (Task) eventKind;
        assertEquals("2", task.getId());
        assertEquals("context-1234", task.getContextId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        List<Artifact> artifacts = task.getArtifacts();
        assertEquals(1, artifacts.size());
        Artifact artifact = artifacts.get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        Part<?> part = artifact.parts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).getText());
    }
}
