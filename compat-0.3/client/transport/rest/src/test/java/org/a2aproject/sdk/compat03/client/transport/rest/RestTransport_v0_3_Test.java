package org.a2aproject.sdk.compat03.client.transport.rest;


import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.CANCEL_TASK_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.CANCEL_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.GET_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SEND_MESSAGE_STREAMING_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.rest.JsonRestMessages_v0_3.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithUri_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3.Kind;
import org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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

public class RestTransport_v0_3_Test {

    private static final Logger log = Logger.getLogger(RestTransport_v0_3_Test.class.getName());
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
        assertEquals("task", result.getKind());
        Task_v0_3 task = (Task_v0_3) result;
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", task.getId());
        assertEquals("context-1234", task.getContextId());
        assertEquals(TaskState_v0_3.SUBMITTED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNull(task.getMetadata());
        assertEquals(true, task.getArtifacts().isEmpty());
        assertEquals(1, task.getHistory().size());
        Message_v0_3 history = task.getHistory().get(0);
        assertEquals("message", history.getKind());
        assertEquals(Message_v0_3.Role.USER, history.getRole());
        assertEquals("context-1234", history.getContextId());
        assertEquals("message-1234", history.getMessageId());
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", history.getTaskId());
        assertEquals(1, history.getParts().size());
        assertEquals(Kind.TEXT, history.getParts().get(0).getKind());
        assertEquals("tell me a joke", ((TextPart_v0_3) history.getParts().get(0)).getText());
        assertNull(history.getMetadata());
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
        ClientCallContext_v0_3 context = null;
        RestTransport_v0_3 instance = new RestTransport_v0_3(CARD);
        Task_v0_3 task = instance.cancelTask(new TaskIdParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64",
                new HashMap<>()), context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals(TaskState_v0_3.CANCELED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNull(task.getMetadata());
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
        ClientCallContext_v0_3 context = null;
        TaskQueryParams_v0_3 request = new TaskQueryParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64", 10);
        RestTransport_v0_3 instance = new RestTransport_v0_3(CARD);
        Task_v0_3 task = instance.getTask(request, context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals(TaskState_v0_3.COMPLETED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNull(task.getMetadata());
        assertEquals(false, task.getArtifacts().isEmpty());
        assertEquals(1, task.getArtifacts().size());
        Artifact_v0_3 artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("", artifact.name());
        assertEquals(false, artifact.parts().isEmpty());
        assertEquals(Kind.TEXT, artifact.parts().get(0).getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) artifact.parts().get(0)).getText());
        assertEquals(1, task.getHistory().size());
        Message_v0_3 history = task.getHistory().get(0);
        assertEquals("message", history.getKind());
        assertEquals(Message_v0_3.Role.USER, history.getRole());
        assertEquals("message-123", history.getMessageId());
        assertEquals(3, history.getParts().size());
        assertEquals(Kind.TEXT, history.getParts().get(0).getKind());
        assertEquals("tell me a joke", ((TextPart_v0_3) history.getParts().get(0)).getText());
        assertEquals(Kind.FILE, history.getParts().get(1).getKind());
        FilePart_v0_3 part = (FilePart_v0_3) history.getParts().get(1);
        assertEquals("text/plain", part.getFile().mimeType());
        assertEquals("file:///path/to/file.txt", ((FileWithUri_v0_3) part.getFile()).uri());
        part = (FilePart_v0_3) history.getParts().get(2);
        assertEquals(Kind.FILE, part.getKind());
        assertEquals("text/plain", part.getFile().mimeType());
        assertEquals("hello", ((FileWithBytes_v0_3) part.getFile()).bytes());
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
        assertEquals("task", receivedEvent.get().getKind());
        Task_v0_3 task = (Task_v0_3) receivedEvent.get();
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
        ClientCallContext_v0_3 context = null;
        RestTransport_v0_3 instance = new RestTransport_v0_3(CARD);
        instance.deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams_v0_3("de38c76d-d54c-436c-8b9f-4c2703648d64", "10"), context);
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
        assertEquals("2", task.getId());
        assertEquals("context-1234", task.getContextId());
        assertEquals(TaskState_v0_3.COMPLETED, task.getStatus().state());
        List<Artifact_v0_3> artifacts = task.getArtifacts();
        assertEquals(1, artifacts.size());
        Artifact_v0_3 artifact = artifacts.get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        Part_v0_3<?> part = artifact.parts().get(0);
        assertEquals(Part_v0_3.Kind.TEXT, part.getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart_v0_3) part).getText());
    }
}
