package io.a2a.client.transport.jsonrpc;

import static io.a2a.client.transport.jsonrpc.JsonStreamingMessages.SEND_MESSAGE_STREAMING_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonStreamingMessages.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static io.a2a.client.transport.jsonrpc.JsonStreamingMessages.TASK_SUBSCRIPTION_TEST_REQUEST;
import static io.a2a.client.transport.jsonrpc.JsonStreamingMessages.TASK_SUBSCRIPTION_REQUEST_TEST_RESPONSE;
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

import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class JSONRPCTransportStreamingTest {

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
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("test message")))
                .contextId("context-test")
                .messageId("message-test")
                .build();

        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();

        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        assertNotNull(params);
        assertEquals(message, params.message());
        assertEquals(configuration, params.configuration());
        assertEquals(Message.Role.USER, params.message().role());
        assertEquals("test message", ((TextPart) params.message().parts().get(0)).text());
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

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me some jokes")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build();
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(message)
                .configuration(configuration)
                .build();

        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<StreamingEventKind> eventHandler = event -> {
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
    public void testA2AClientSubscribeToTask() throws Exception {
        this.server.when(request()
                                .withMethod("POST")
                                .withPath("/")
                                .withBody(JsonBody.json(TASK_SUBSCRIPTION_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))

                )
                .respond(response()
                                .withStatusCode(200)
                                .withHeader("Content-Type", "text/event-stream")
                                .withBody(TASK_SUBSCRIPTION_REQUEST_TEST_RESPONSE)
                );

        JSONRPCTransport client = new JSONRPCTransport("http://localhost:4001");
        TaskIdParams taskIdParams = new TaskIdParams("task-1234");

        AtomicReference<StreamingEventKind> receivedEvent = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Consumer<StreamingEventKind> eventHandler = event -> {
            receivedEvent.set(event);
            latch.countDown();
        };
        Consumer<Throwable> errorHandler = error -> {};
        client.subscribeToTask(taskIdParams, eventHandler, errorHandler, null);

        boolean eventReceived = latch.await(10, TimeUnit.SECONDS);
        assertTrue(eventReceived);

        StreamingEventKind eventKind = receivedEvent.get();;
        assertNotNull(eventKind);
        assertInstanceOf(Task.class, eventKind);
        Task task = (Task) eventKind;
        assertEquals("2", task.id());
        assertEquals("context-1234", task.contextId());
        assertEquals(TaskState.COMPLETED, task.status().state());
        List<Artifact> artifacts = task.artifacts();
        assertEquals(1, artifacts.size());
        Artifact artifact = artifacts.get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("joke", artifact.name());
        Part<?> part = artifact.parts().get(0);
        assertTrue(part instanceof TextPart);
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart) part).text());
    }
} 