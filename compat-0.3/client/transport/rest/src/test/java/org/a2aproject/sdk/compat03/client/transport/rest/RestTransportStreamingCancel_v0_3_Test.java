package org.a2aproject.sdk.compat03.client.transport.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

/**
 * Covers the ordering the REST SSE transport has to survive: {@code postAsyncSSE} may deliver
 * events before it returns, so the listener can see a terminal event while the streaming future is
 * still unpublished. REST never replays the request, so a missed close would leave the caller
 * waiting on a stream that already ended.
 */
public class RestTransportStreamingCancel_v0_3_Test {

    private static final String FINAL_STATUS_UPDATE = """
            {
              "statusUpdate": {
                "taskId": "task-1234",
                "contextId": "context-1234",
                "status": {
                  "state": "TASK_STATE_COMPLETED"
                },
                "final": true
              }
            }""";

    private static final String RUNNING_STATUS_UPDATE = """
            {
              "statusUpdate": {
                "taskId": "task-1234",
                "contextId": "context-1234",
                "status": {
                  "state": "TASK_STATE_WORKING"
                },
                "final": false
              }
            }""";

    private static final AgentCard_v0_3 CARD = new AgentCard_v0_3.Builder()
            .name("Hello World Agent")
            .description("Just a hello world agent")
            .url("http://localhost:4001")
            .version("1.0.0")
            .capabilities(new AgentCapabilities_v0_3.Builder()
                    .streaming(true)
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

    @Test
    public void testFinalEventDeliveredBeforePostAsyncSseReturns() throws Exception {
        SynchronousSseHttpClient httpClient = new SynchronousSseHttpClient(FINAL_STATUS_UPDATE);
        RestTransport_v0_3 transport = transport(httpClient);

        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        transport.sendMessageStreaming(streamingParams(), receivedEvent::set, receivedError::set, null);

        TaskStatusUpdateEvent_v0_3 event =
                assertInstanceOf(TaskStatusUpdateEvent_v0_3.class, receivedEvent.get());
        assertTrue(event.isFinal());
        assertNull(receivedError.get(), "A final status update ends the stream normally");
        assertTrue(httpClient.streamFuture.isCancelled(),
                "A stream whose final event arrives before the future is published still has to be closed");
    }

    @Test
    public void testResubscribeOfFinalTaskDeliveredBeforePostAsyncSseReturns() throws Exception {
        SynchronousSseHttpClient httpClient = new SynchronousSseHttpClient(FINAL_STATUS_UPDATE);
        RestTransport_v0_3 transport = transport(httpClient);

        AtomicInteger events = new AtomicInteger();
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        transport.resubscribe(new TaskIdParams_v0_3("task-1234"), event -> events.incrementAndGet(),
                receivedError::set, null);

        assertEquals(1, events.get());
        assertNull(receivedError.get(), "A final status update ends the stream normally");
        assertTrue(httpClient.streamFuture.isCancelled(),
                "Resubscription closes the channel the same way as a new streaming message");
    }

    @Test
    public void testNonFinalEventDeliveredBeforePostAsyncSseReturnsKeepsTheStreamOpen() throws Exception {
        SynchronousSseHttpClient httpClient = new SynchronousSseHttpClient(RUNNING_STATUS_UPDATE);
        RestTransport_v0_3 transport = transport(httpClient);

        AtomicReference<StreamingEventKind_v0_3> receivedEvent = new AtomicReference<>();
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        transport.sendMessageStreaming(streamingParams(), receivedEvent::set, receivedError::set, null);

        TaskStatusUpdateEvent_v0_3 event =
                assertInstanceOf(TaskStatusUpdateEvent_v0_3.class, receivedEvent.get());
        assertFalse(event.isFinal());
        assertNull(receivedError.get());
        assertFalse(httpClient.streamFuture.isCancelled(),
                "A stream that is still running must not be closed");
    }

    private static RestTransport_v0_3 transport(A2AHttpClient httpClient) {
        return new RestTransport_v0_3(httpClient, CARD, "http://localhost:4001", null);
    }

    private static MessageSendParams_v0_3 streamingParams() {
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
        return new MessageSendParams_v0_3.Builder()
                .message(message)
                .configuration(configuration)
                .build();
    }

    /**
     * Delivers the single SSE event synchronously from {@code postAsyncSSE}, so the caller has no
     * chance to publish the returned future before the listener sees it.
     */
    private static final class SynchronousSseHttpClient implements A2AHttpClient {

        private final String eventJson;
        private final CompletableFuture<Void> streamFuture = new CompletableFuture<>();

        private SynchronousSseHttpClient(String eventJson) {
            this.eventJson = eventJson;
        }

        @Override
        public GetBuilder createGet() {
            throw new UnsupportedOperationException("This test client only streams POST requests");
        }

        @Override
        public PostBuilder createPost() {
            return new PostBuilder() {
                @Override
                public PostBuilder body(String body) {
                    return this;
                }

                @Override
                public PostBuilder url(String s) {
                    return this;
                }

                @Override
                public PostBuilder addHeaders(Map<String, String> headers) {
                    return this;
                }

                @Override
                public PostBuilder addHeader(String name, String value) {
                    return this;
                }

                @Override
                public A2AHttpResponse post() {
                    throw new UnsupportedOperationException("This test client only streams POST requests");
                }

                @Override
                public CompletableFuture<Void> postAsyncSSE(Consumer<ServerSentEvent> messageConsumer,
                                                            Consumer<Throwable> errorConsumer,
                                                            Runnable completeRunnable) {
                    messageConsumer.accept(new ServerSentEvent(eventJson));
                    return streamFuture;
                }
            };
        }

        @Override
        public DeleteBuilder createDelete() {
            throw new UnsupportedOperationException("This test client only streams POST requests");
        }
    }
}
