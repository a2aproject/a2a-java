package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonStreamingMessages_v0_3.SEND_MESSAGE_STREAMING_TEST_REQUEST;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonStreamingMessages_v0_3.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonStreamingMessages_v0_3.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
import static org.a2aproject.sdk.compat03.client.transport.jsonrpc.JsonStreamingMessages_v0_3.TASK_RESUBSCRIPTION_TEST_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class JSONRPCTransportStreaming_v0_3_Test {

    private ClientAndServer server;

    @BeforeEach
    public void setUp() {
        server = new ClientAndServer(4001);
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testSendStreamingMessageParams() {
        // The goal here is just to verify the correct parameters are being used
        // This is a unit test of the parameter construction, not the streaming itself
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.USER)
                .parts(Collections.singletonList(new TextPart_v0_3("test message")))
                .contextId("context-test")
                .messageId("message-test")
                .build();

        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();

        MessageSendParams_v0_3 params = new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();

        assertNotNull(params);
        assertEquals(message, params.message());
        assertEquals(configuration, params.configuration());
        assertEquals(Message_v0_3.Role.USER, params.message().getRole());
        assertEquals("test message", ((TextPart_v0_3) params.message().getParts().get(0)).getText());
    }

    @Test
    public void testA2AClientSendStreamingMessage() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(SEND_MESSAGE_STREAMING_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(SEND_MESSAGE_STREAMING_TEST_RESPONSE)
                );

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
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
        Consumer<Throwable> errorHandler = error -> {};
        client.sendMessageStreaming(params, eventHandler, errorHandler, null);

        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(eventReceived);
        assertNotNull(receivedEvent.get());
    }

    @Test
    public void testA2AClientResubscribeToTask() throws Exception {
        this.server.when(
                        request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(TASK_RESUBSCRIPTION_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE)
                );

        JSONRPCTransport_v0_3 client = new JSONRPCTransport_v0_3("http://localhost:4001");
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