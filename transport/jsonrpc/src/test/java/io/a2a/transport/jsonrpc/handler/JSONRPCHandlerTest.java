package io.a2a.transport.jsonrpc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.jsonrpc.common.wrappers.CancelTaskRequest;
import io.a2a.jsonrpc.common.wrappers.CancelTaskResponse;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardRequest;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskResponse;
import io.a2a.jsonrpc.common.wrappers.ListTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.ListTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksRequest;
import io.a2a.jsonrpc.common.wrappers.ListTasksResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.jsonrpc.common.wrappers.SendMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SetTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.SetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.MainEventBusProcessorCallback;
import io.a2a.server.requesthandlers.AbstractA2ARequestHandlerTest;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Artifact;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.Event;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.ExtensionSupportRequiredError;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.spec.VersionNotSupportedError;
import mutiny.zero.ZeroPublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class JSONRPCHandlerTest extends AbstractA2ARequestHandlerTest {

    private final ServerCallContext callContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

    @Test
    public void testOnGetTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.id()));
        GetTaskResponse response = handler.onGetTask(request, callContext);
        assertEquals(request.getId(), response.getId());
        Assertions.assertSame(MINIMAL_TASK, response.getResult());
        assertNull(response.getError());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.id()));
        GetTaskResponse response = handler.onGetTask(request, callContext);
        assertEquals(request.getId(), response.getId());
        assertInstanceOf(TaskNotFoundError.class, response.getError());
        assertNull(response.getResult());
    }

    @Test
    public void testOnCancelTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            Task task = context.getTask();
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
            taskUpdater.cancel();
        };

        CancelTaskRequest request = new CancelTaskRequest("111", new TaskIdParams(MINIMAL_TASK.id()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);

        assertNull(response.getError());
        assertEquals(request.getId(), response.getId());
        Task task = response.getResult();
        assertEquals(MINIMAL_TASK.id(), task.id());
        assertEquals(MINIMAL_TASK.contextId(), task.contextId());
        assertEquals(TaskState.CANCELED, task.status().state());
    }

    @Test
    public void testOnCancelTaskNotSupported() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            throw new UnsupportedOperationError();
        };

        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.id()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
    }

    @Test
    public void testOnCancelTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.id()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(TaskNotFoundError.class, response.getError());
    }

    @Test
    public void testOnMessageNewMessageSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        assertNull(response.getError());
        Assertions.assertSame(message, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        assertNull(response.getError());
        Assertions.assertSame(message, response.getResult());
    }

    @Test
    public void testOnMessageError() {
        // See testMessageOnErrorMocks() for a test more similar to the Python implementation, using mocks for
        // EventConsumer.consumeAll()
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(new UnsupportedOperationError());
        };
        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertNull(response.getResult());
    }

    @Test
    public void testOnMessageErrorMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromItems(wrapEvent(new UnsupportedOperationError()))).when(mock).consumeAll();
                })) {
            response = handler.onMessageSend(request, callContext);
        }

        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertNull(response.getResult());
    }

    @Test
    public void testOnMessageStreamNewMessageSuccess() throws InterruptedException {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        Message message = Message.builder(MESSAGE)
            .taskId(MINIMAL_TASK.id())
            .contextId(MINIMAL_TASK.contextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        List<StreamingEventKind> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item.getResult());
                subscription.request(1);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        latch.await();

        // The Python implementation has several events emitted since it uses mocks. Also, in the
        // implementation, a Message is considered a 'final' Event in EventConsumer.consumeAll()
        // so there would be no more Events.
        //
        // See testOnMessageStreamNewMessageSuccessMocks() for a test more similar to the Python implementation
        assertEquals(1, results.size());
        Assertions.assertSame(message, results.get(0));
    }

    @Test
    public void testOnMessageStreamNewMessageMultipleEventsSuccess() throws InterruptedException {
        // Setup callback to wait for all 3 events to be processed by MainEventBusProcessor
        CountDownLatch processingLatch = new CountDownLatch(3);
        mainEventBusProcessor.setCallback(new MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event event) {
                processingLatch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
                // Not needed for this test
            }
        });

        try {
            JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

            // Create multiple events to be sent during streaming
        Task taskEvent = Task.builder(MINIMAL_TASK)
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        TaskArtifactUpdateEvent artifactEvent = TaskArtifactUpdateEvent.builder()
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .artifact(Artifact.builder()
                        .artifactId("artifact-1")
                        .parts(new TextPart("Generated artifact content"))
                        .build())
                .build();

        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();

        // Configure the agent executor to enqueue multiple events
        agentExecutorExecute = (context, eventQueue) -> {
            // Enqueue the task with WORKING state
            eventQueue.enqueueEvent(taskEvent);
            // Enqueue an artifact update event
            eventQueue.enqueueEvent(artifactEvent);
            // Enqueue a status update event to complete the task (this is the "final" event)
            eventQueue.enqueueEvent(statusEvent);
        };

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        List<StreamingEventKind> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3); // Expect 3 events
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item.getResult());
                subscription.request(1);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
                // Release latch to prevent timeout
                while (latch.getCount() > 0) {
                    latch.countDown();
                }
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

            // Wait for all events to be received (increased timeout for async processing)
            Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS),
                    "Expected to receive 3 events within timeout");

            // Wait for MainEventBusProcessor to complete processing all 3 events
            Assertions.assertTrue(processingLatch.await(5, TimeUnit.SECONDS),
                    "MainEventBusProcessor should have processed all 3 events");

            // Assert no error occurred during streaming
            Assertions.assertNull(error.get(), "No error should occur during streaming");

            // Verify that all 3 events were received
            assertEquals(3, results.size(), "Should have received exactly 3 events");

            // Verify the first event is the task
            Task receivedTask = assertInstanceOf(Task.class, results.get(0), "First event should be a Task");
            assertEquals(MINIMAL_TASK.id(), receivedTask.id());
            assertEquals(MINIMAL_TASK.contextId(), receivedTask.contextId());
            assertEquals(TaskState.WORKING, receivedTask.status().state());

            // Verify the second event is the artifact update
            TaskArtifactUpdateEvent receivedArtifact = assertInstanceOf(TaskArtifactUpdateEvent.class, results.get(1),
                    "Second event should be a TaskArtifactUpdateEvent");
            assertEquals(MINIMAL_TASK.id(), receivedArtifact.taskId());
            assertEquals("artifact-1", receivedArtifact.artifact().artifactId());

            // Verify the third event is the status update
            TaskStatusUpdateEvent receivedStatus = assertInstanceOf(TaskStatusUpdateEvent.class, results.get(2),
                    "Third event should be a TaskStatusUpdateEvent");
            assertEquals(MINIMAL_TASK.id(), receivedStatus.taskId());
            assertEquals(TaskState.COMPLETED, receivedStatus.status().state());
        } finally {
            mainEventBusProcessor.setCallback(null);
        }
    }

    @Test
    public void testOnMessageStreamNewMessageSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        // This is used to send events from a mock
        List<Event> events = List.of(
                MINIMAL_TASK,
                TaskArtifactUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.id())
                        .contextId(MINIMAL_TASK.contextId())
                        .artifact(Artifact.builder()
                                .artifactId("art1")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.id())
                        .contextId(MINIMAL_TASK.contextId())
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .build());

        Message message = Message.builder(MESSAGE)
            .taskId(MINIMAL_TASK.id())
            .contextId(MINIMAL_TASK.contextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events.stream().map(AbstractA2ARequestHandlerTest::wrapEvent).toList())).when(mock).consumeAll();
                })) {
            response = handler.onMessageSendStream(request, callContext);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        List<Event> results = new ArrayList<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add((Event) item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(null);
            }
        });

        future.join();
        Assertions.assertEquals(events, results);
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        Task task = Task.builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(task);

        Message message = Message.builder(MESSAGE)
            .taskId(task.id())
            .contextId(task.contextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        // This Publisher never completes so we subscribe in a new thread.
        // I _think_ that is as expected, and testOnMessageStreamNewMessageSendPushNotificationSuccess seems
        // to confirm this
        final List<StreamingEventKind> results = new ArrayList<>();
        final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Executors.newSingleThreadExecutor().execute(() -> {
            response.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriptionRef.set(subscription);
                    subscription.request(1);
                }

                @Override
                public void onNext(SendStreamingMessageResponse item) {
                    results.add(item.getResult());
                    subscriptionRef.get().request(1);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    subscriptionRef.get().cancel();
                }

                @Override
                public void onComplete() {
                    subscriptionRef.get().cancel();
                }
            });
        });

        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
        subscriptionRef.get().cancel();
        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        Task expected = Task.builder(task)
                .history(message)
                .build();
        assertEquals(1, results.size());
        StreamingEventKind receivedType = results.get(0);
        assertInstanceOf(Task.class, receivedType);
        Task received = (Task) receivedType;
        assertEquals(expected.id(), received.id());
        assertEquals(expected.contextId(), received.contextId());
        assertEquals(expected.status(), received.status());
        assertEquals(expected.history(), received.history());
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        Task task = Task.builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(task);

        // This is used to send events from a mock
        List<Event> events = List.of(
                TaskArtifactUpdateEvent.builder()
                        .taskId(task.id())
                        .contextId(task.contextId())
                        .artifact(Artifact.builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(task.id())
                        .contextId(task.contextId())
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        Message message = Message.builder(MESSAGE)
            .taskId(task.id())
            .contextId(task.contextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events.stream().map(AbstractA2ARequestHandlerTest::wrapEvent).toList())).when(mock).consumeAll();
                })) {
            response = handler.onMessageSendStream(request, callContext);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        List<Event> results = new ArrayList<>();

        // Unlike testOnMessageStreamNewMessageExistingTaskSuccess() the ZeroPublisher.fromIterable()
        // used to mock the events completes once it has sent all the items. So no special thread
        // handling is needed.
        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add((Event) item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(null);
            }
        });

        future.join();

        Assertions.assertEquals(events, results);
    }

    @Test
    public void testSetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        TaskPushNotificationConfig taskPushConfig
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(), 
                        PushNotificationConfig.builder().url("http://example.com")
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4").build(), 
                        "tenant");
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);
        TaskPushNotificationConfig taskPushConfigResult
                = new TaskPushNotificationConfig( MINIMAL_TASK.id(), 
                        PushNotificationConfig.builder().url("http://example.com")
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4").build(),
                        "tenant");

        Assertions.assertEquals(taskPushConfigResult, response.getResult());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(), PushNotificationConfig.builder()
                        .id("c295ea44-7543-4f78-b524-7a38915ad6e4").url("http://example.com").build(), "tenant");

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        GetTaskPushNotificationConfigRequest getRequest
                = new GetTaskPushNotificationConfigRequest("111", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.id()));
        GetTaskPushNotificationConfigResponse getResponse = handler.getPushNotificationConfig(getRequest, callContext);

        TaskPushNotificationConfig expectedConfig = new TaskPushNotificationConfig(MINIMAL_TASK.id(),
                PushNotificationConfig.builder().id("c295ea44-7543-4f78-b524-7a38915ad6e4").url("http://example.com").build(), "");

        assertEquals(expectedConfig, getResponse.getResult());
    }

    @Test
    public void testOnMessageStreamNewMessageSendPushNotificationSuccess() throws Exception {
        // Setup callback to wait for all 3 events to be processed by MainEventBusProcessor
        CountDownLatch processingLatch = new CountDownLatch(3);
        mainEventBusProcessor.setCallback(new MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event event) {
                processingLatch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
                // Not needed for this test
            }
        });

        try {
            JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
            taskStore.save(MINIMAL_TASK);

            List<Event> events = List.of(
                    MINIMAL_TASK,
                    TaskArtifactUpdateEvent.builder()
                            .taskId(MINIMAL_TASK.id())
                            .contextId(MINIMAL_TASK.contextId())
                            .artifact(Artifact.builder()
                                    .artifactId("11")
                                    .parts(new TextPart("text"))
                                    .build())
                            .build(),
                    TaskStatusUpdateEvent.builder()
                            .taskId(MINIMAL_TASK.id())
                            .contextId(MINIMAL_TASK.contextId())
                            .status(new TaskStatus(TaskState.COMPLETED))
                            .build());


            agentExecutorExecute = (context, eventQueue) -> {
                // Hardcode the events to send here
                for (Event event : events) {
                    eventQueue.enqueueEvent(event);
                }
            };

            TaskPushNotificationConfig config = new TaskPushNotificationConfig(
                    MINIMAL_TASK.id(),
                    PushNotificationConfig.builder().id("c295ea44-7543-4f78-b524-7a38915ad6e4").url("http://example.com").build(), "tenant");

            SetTaskPushNotificationConfigRequest stpnRequest = new SetTaskPushNotificationConfigRequest("1", config);
            SetTaskPushNotificationConfigResponse stpnResponse = handler.setPushNotificationConfig(stpnRequest, callContext);
            assertNull(stpnResponse.getError());

            Message msg = Message.builder(MESSAGE)
                    .taskId(MINIMAL_TASK.id())
                    .build();
            SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(msg, null, null));
            Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

            final List<StreamingEventKind> results = Collections.synchronizedList(new ArrayList<>());
            final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
            final CountDownLatch latch = new CountDownLatch(6);
            httpClient.latch = latch;

            Executors.newSingleThreadExecutor().execute(() -> {
                response.subscribe(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscriptionRef.set(subscription);
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(SendStreamingMessageResponse item) {
                        System.out.println("-> " + item.getResult());
                        results.add(item.getResult());
                        System.out.println(results);
                        subscriptionRef.get().request(1);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriptionRef.get().cancel();
                    }

                    @Override
                    public void onComplete() {
                        subscriptionRef.get().cancel();
                    }
                });
            });

            Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));

            // Wait for MainEventBusProcessor to complete processing all 3 events
            Assertions.assertTrue(processingLatch.await(5, TimeUnit.SECONDS),
                    "MainEventBusProcessor should have processed all 3 events");

            subscriptionRef.get().cancel();
            assertEquals(3, results.size());
            assertEquals(3, httpClient.tasks.size());

            Task curr = httpClient.tasks.get(0);
            assertEquals(MINIMAL_TASK.id(), curr.id());
            assertEquals(MINIMAL_TASK.contextId(), curr.contextId());
            assertEquals(MINIMAL_TASK.status().state(), curr.status().state());
            assertEquals(0, curr.artifacts() == null ? 0 : curr.artifacts().size());

            curr = httpClient.tasks.get(1);
            assertEquals(MINIMAL_TASK.id(), curr.id());
            assertEquals(MINIMAL_TASK.contextId(), curr.contextId());
            assertEquals(MINIMAL_TASK.status().state(), curr.status().state());
            assertEquals(1, curr.artifacts().size());
            assertEquals(1, curr.artifacts().get(0).parts().size());
            assertEquals("text", ((TextPart) curr.artifacts().get(0).parts().get(0)).text());

            curr = httpClient.tasks.get(2);
            assertEquals(MINIMAL_TASK.id(), curr.id());
            assertEquals(MINIMAL_TASK.contextId(), curr.contextId());
            assertEquals(TaskState.COMPLETED, curr.status().state());
            assertEquals(1, curr.artifacts().size());
            assertEquals(1, curr.artifacts().get(0).parts().size());
            assertEquals("text", ((TextPart) curr.artifacts().get(0).parts().get(0)).text());
        } finally {
            mainEventBusProcessor.setCallback(null);
        }
    }

    @Test
    public void testOnResubscribeExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        queueManager.createOrTap(MINIMAL_TASK.id());

        agentExecutorExecute = (context, eventQueue) -> {
            // The only thing hitting the agent is the onMessageSend() and we should use the message
            eventQueue.enqueueEvent(context.getMessage());
            //eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.id()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onSubscribeToTask(request, callContext);

        // We need to send some events in order for those to end up in the queue
        Message message = Message.builder()
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .role(Message.Role.AGENT)
                .parts(new TextPart("text"))
                .build();
        SendMessageResponse smr
                = handler.onMessageSend(
                        new SendMessageRequest("1", new MessageSendParams(message, null, null)),
                        callContext);
        assertNull(smr.getError());

        CompletableFuture<Void> future = new CompletableFuture<>();
        List<StreamingEventKind> results = new ArrayList<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                subscription.cancel();
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                subscription.cancel();
                future.complete(null);
            }
        });

        future.join();

        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        assertEquals(1, results.size());
    }

    @Test
    public void testOnResubscribeExistingTaskSuccessMocks() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        queueManager.createOrTap(MINIMAL_TASK.id());

        List<Event> events = List.of(
                TaskArtifactUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.id())
                        .contextId(MINIMAL_TASK.contextId())
                        .artifact(Artifact.builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.id())
                        .contextId(MINIMAL_TASK.contextId())
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.id()));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events.stream().map(AbstractA2ARequestHandlerTest::wrapEvent).toList())).when(mock).consumeAll();
                })) {
            response = handler.onSubscribeToTask(request, callContext);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        List<StreamingEventKind> results = new ArrayList<>();

        // Unlike testOnResubscribeExistingTaskSuccess() the ZeroPublisher.fromIterable()
        // used to mock the events completes once it has sent all the items. So no special thread
        // handling is needed.
        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item.getResult());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                subscription.cancel();
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                subscription.cancel();
                future.complete(null);
            }
        });

        future.join();

        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        assertEquals(events, results);
    }

    @Test
    public void testOnResubscribeNoExistingTaskError() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.id()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onSubscribeToTask(request, callContext);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        assertNull(results.get(0).getResult());
        assertInstanceOf(TaskNotFoundError.class, results.get(0).getError());
    }

    @Test
    public void testStreamingNotSupportedError() {
        AgentCard card = createAgentCard(false, true, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        SendStreamingMessageRequest request = SendStreamingMessageRequest.builder()
                .id("1")
                .params(MessageSendParams.builder()
                        .message(MESSAGE)
                        .build())
                .build();
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        if (results.get(0).getError() != null && results.get(0).getError() instanceof InvalidRequestError ire) {
            assertEquals("Streaming is not supported by the agent", ire.getMessage());
        } else {
            Assertions.fail("Expected a response containing an error");
        }
    }

    @Test
    public void testStreamingNotSupportedErrorOnResubscribeToTask() {
        // This test does not exist in the Python implementation
        AgentCard card = createAgentCard(false, true, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.id()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onSubscribeToTask(request, callContext);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        if (results.get(0).getError() != null && results.get(0).getError() instanceof InvalidRequestError ire) {
            assertEquals("Streaming is not supported by the agent", ire.getMessage());
        } else {
            Assertions.fail("Expected a response containing an error");
        }
    }

    @Test
    public void testPushNotificationsNotSupportedError() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        TaskPushNotificationConfig config
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(),
                        PushNotificationConfig.builder()
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4")
                                .url("http://example.com")
                                .build(), "tenant");

        SetTaskPushNotificationConfigRequest request = SetTaskPushNotificationConfigRequest.builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);
        assertInstanceOf(PushNotificationNotSupportedError.class, response.getError());
    }

    @Test
    public void testOnGetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(executor, taskStore, queueManager, null, internalExecutor, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        taskStore.save(MINIMAL_TASK);

        GetTaskPushNotificationConfigRequest request
                = new GetTaskPushNotificationConfigRequest("id", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.id()));
        GetTaskPushNotificationConfigResponse response = handler.getPushNotificationConfig(request, callContext);

        Assertions.assertNotNull(response.getError());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnSetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(executor, taskStore, queueManager, null, internalExecutor, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        taskStore.save(MINIMAL_TASK);

        TaskPushNotificationConfig config
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(),
                        PushNotificationConfig.builder()
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4")
                                .url("http://example.com")
                                .build(), "tenant");

        SetTaskPushNotificationConfigRequest request = SetTaskPushNotificationConfigRequest.builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);

        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnMessageSendInternalError() {
        DefaultRequestHandler mocked = Mockito.mock(DefaultRequestHandler.class);
        Mockito.doThrow(new InternalError("Internal Error")).when(mocked)
                .onMessageSend(Mockito.any(MessageSendParams.class), Mockito.any(ServerCallContext.class));

        JSONRPCHandler handler = new JSONRPCHandler(CARD, mocked, internalExecutor);

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);

        assertInstanceOf(InternalError.class, response.getError());
    }

    @Test
    public void testOnMessageStreamInternalError() {
        DefaultRequestHandler mocked = Mockito.mock(DefaultRequestHandler.class);
        Mockito.doThrow(new InternalError("Internal Error")).when(mocked)
                .onMessageSendStream(Mockito.any(MessageSendParams.class), Mockito.any(ServerCallContext.class));

        JSONRPCHandler handler = new JSONRPCHandler(CARD, mocked, internalExecutor);

        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        assertInstanceOf(InternalError.class, results.get(0).getError());
    }

    @Test
    @Disabled
    public void testDefaultRequestHandlerWithCustomComponents() {
        // Not much happening in the Python test beyond checking that the DefaultRequestHandler
        // constructor sets the fields as expected
    }

    @Test
    public void testOnMessageSendErrorHandling() {
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(executor, taskStore, queueManager, null, internalExecutor, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        taskStore.save(MINIMAL_TASK);

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;

        try (MockedConstruction<ResultAggregator> mocked = Mockito.mockConstruction(
                ResultAggregator.class,
                (mock, context) -> {
                    Mockito.doThrow(
                            new UnsupportedOperationError())
                            .when(mock).consumeAndBreakOnInterrupt(
                            Mockito.any(EventConsumer.class),
                            Mockito.anyBoolean());
                })) {
            response = handler.onMessageSend(request, callContext);
        }

        assertInstanceOf(UnsupportedOperationError.class, response.getError());

    }

    @Test
    public void testOnMessageSendTaskIdMismatch() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        agentExecutorExecute = ((context, eventQueue) -> {
            eventQueue.enqueueEvent(MINIMAL_TASK);
        });
        SendMessageRequest request = new SendMessageRequest("1",
                new MessageSendParams(MESSAGE, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        assertInstanceOf(InternalError.class, response.getError());

    }

    @Test
    public void testOnMessageStreamTaskIdMismatch() throws InterruptedException {
        // Setup callback to wait for the 1 event to be processed by MainEventBusProcessor
        CountDownLatch processingLatch = new CountDownLatch(1);
        mainEventBusProcessor.setCallback(new MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event event) {
                processingLatch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
                // Not needed for this test
            }
        });

        try {
            JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
            taskStore.save(MINIMAL_TASK);

            agentExecutorExecute = ((context, eventQueue) -> {
                eventQueue.enqueueEvent(MINIMAL_TASK);
            });

            SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
            Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        CompletableFuture<Void> future = new CompletableFuture<>();
        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                subscription.cancel();
                future.complete(null);
            }
        });

            future.join();

            // Wait for MainEventBusProcessor to complete processing the event
            Assertions.assertTrue(processingLatch.await(5, TimeUnit.SECONDS),
                    "MainEventBusProcessor should have processed the event");

            Assertions.assertNull(error.get());
            Assertions.assertEquals(1, results.size());
            Assertions.assertInstanceOf(InternalError.class, results.get(0).getError());
        } finally {
            mainEventBusProcessor.setCallback(null);
        }
    }

    @Test
    public void testListPushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.id())
                        .build(), "tenant");
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);
        TaskPushNotificationConfig result = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.id())
                        .build(), "");
        ListTaskPushNotificationConfigRequest listRequest
                = new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.id()));
        ListTaskPushNotificationConfigResponse listResponse = handler.listPushNotificationConfig(listRequest, callContext);

        assertEquals("111", listResponse.getId());
        assertEquals(1, listResponse.getResult().size());
        assertEquals(result, listResponse.getResult().configs().get(0));
    }

    @Test
    public void testListPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.id())
                        .build(), "tenant");
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        ListTaskPushNotificationConfigRequest listRequest
                = new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.id()));
        ListTaskPushNotificationConfigResponse listResponse
                = handler.listPushNotificationConfig(listRequest, callContext);

        assertEquals("111", listResponse.getId());
        assertNull(listResponse.getResult());
        assertInstanceOf(PushNotificationNotSupportedError.class, listResponse.getError());
    }

    @Test
    public void testListPushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(executor, taskStore, queueManager, null, internalExecutor, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest listRequest
                = new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.id()));
        ListTaskPushNotificationConfigResponse listResponse
                = handler.listPushNotificationConfig(listRequest, callContext);

        assertEquals("111", listResponse.getId());
        assertNull(listResponse.getResult());
        assertInstanceOf(UnsupportedOperationError.class, listResponse.getError());
    }

    @Test
    public void testListPushNotificationConfigTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest listRequest
                = new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.id()));
        ListTaskPushNotificationConfigResponse listResponse
                = handler.listPushNotificationConfig(listRequest, callContext);

        assertEquals("111", listResponse.getId());
        assertNull(listResponse.getResult());
        assertInstanceOf(TaskNotFoundError.class, listResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.id())
                        .build(), "tenant");
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest
                = new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.id(), MINIMAL_TASK.id()));
        DeleteTaskPushNotificationConfigResponse deleteResponse
                = handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getError());
        assertNull(deleteResponse.getResult());
    }

    @Test
    public void testDeletePushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.id())
                        .build(), "tenant");
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest
                = new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.id(), MINIMAL_TASK.id()));
        DeleteTaskPushNotificationConfigResponse deleteResponse
                = handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(PushNotificationNotSupportedError.class, deleteResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler =
                DefaultRequestHandler.create(executor, taskStore, queueManager, null, internalExecutor, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig
                = new TaskPushNotificationConfig(
                        MINIMAL_TASK.id(),  PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.id())
                        .build(), "tenant");
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest
                = new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.id(), MINIMAL_TASK.id()));
        DeleteTaskPushNotificationConfigResponse deleteResponse
                = handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(UnsupportedOperationError.class, deleteResponse.getError());
    }

    @Test
    public void testOnGetExtendedAgentCard() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        GetExtendedAgentCardRequest request = new GetExtendedAgentCardRequest("1");
        GetExtendedAgentCardResponse response = handler.onGetExtendedCardRequest(request, callContext);
        assertEquals(request.getId(), response.getId());
        assertInstanceOf(ExtendedAgentCardNotConfiguredError.class, response.getError());
        assertNull(response.getResult());
    }

    @Test
    public void testStreamingDoesNotBlockMainThread() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        // Track if the main thread gets blocked during streaming
        AtomicBoolean mainThreadBlocked = new AtomicBoolean(true);
        AtomicBoolean eventReceived = new AtomicBoolean(false);
        CountDownLatch streamStarted = new CountDownLatch(1);
        CountDownLatch eventProcessed = new CountDownLatch(1);

        agentExecutorExecute = (context, eventQueue) -> {
            // Wait a bit to ensure the main thread continues
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            eventQueue.enqueueEvent(context.getMessage());
        };

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(message, null, null));

        // Start streaming
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                streamStarted.countDown();
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                eventReceived.set(true);
                eventProcessed.countDown();
                subscription.cancel();
            }

            @Override
            public void onError(Throwable throwable) {
                subscription.cancel();
                eventProcessed.countDown();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
                eventProcessed.countDown();
            }
        });

        // The main thread should not be blocked - we should be able to continue immediately
        Assertions.assertTrue(streamStarted.await(100, TimeUnit.MILLISECONDS),
                "Streaming subscription should start quickly without blocking main thread");

        // This proves the main thread is not blocked - we can do other work
        // Simulate main thread doing other work
        Thread.sleep(50);

        mainThreadBlocked.set(false); // If we get here, main thread was not blocked

        // Wait for the actual event processing to complete
        Assertions.assertTrue(eventProcessed.await(2, TimeUnit.SECONDS),
                "Event should be processed within reasonable time");

        // Verify we received the event and main thread was not blocked
        Assertions.assertTrue(eventReceived.get(), "Should have received streaming event");
        Assertions.assertFalse(mainThreadBlocked.get(), "Main thread should not have been blocked");
    }

    @Test
    public void testExtensionSupportRequiredErrorOnMessageSend() {
        // Create AgentCard with a required extension
        AgentCard cardWithExtension = AgentCard.builder()
                .name("test-card")
                .description("Test card with required extension")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extensions(List.of(
                                AgentExtension.builder()
                                        .uri("https://example.com/test-extension")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .protocolVersions(AgentCard.CURRENT_PROTOCOL_VERSION)
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(cardWithExtension, requestHandler, internalExecutor);

        // Use callContext which has empty requestedExtensions set
        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);

        assertInstanceOf(ExtensionSupportRequiredError.class, response.getError());
        Assertions.assertTrue(response.getError().getMessage().contains("https://example.com/test-extension"));
        assertNull(response.getResult());
    }

    @Test
    public void testExtensionSupportRequiredErrorOnMessageSendStream() {
        // Create AgentCard with a required extension
        AgentCard cardWithExtension = AgentCard.builder()
                .name("test-card")
                .description("Test card with required extension")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extensions(List.of(
                                AgentExtension.builder()
                                        .uri("https://example.com/streaming-extension")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .protocolVersions(AgentCard.CURRENT_PROTOCOL_VERSION)
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(cardWithExtension, requestHandler, internalExecutor);

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        assertEquals(1, results.size());
        assertInstanceOf(ExtensionSupportRequiredError.class, results.get(0).getError());
        Assertions.assertTrue(results.get(0).getError().getMessage().contains("https://example.com/streaming-extension"));
        assertNull(results.get(0).getResult());
    }

    @Test
    public void testRequiredExtensionProvidedSuccess() {
        // Create AgentCard with a required extension
        AgentCard cardWithExtension = AgentCard.builder()
                .name("test-card")
                .description("Test card with required extension")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extensions(List.of(
                                AgentExtension.builder()
                                        .uri("https://example.com/required-extension")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .protocolVersions(AgentCard.CURRENT_PROTOCOL_VERSION)
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(cardWithExtension, requestHandler, internalExecutor);

        // Create context WITH the required extension
        Set<String> requestedExtensions = new HashSet<>();
        requestedExtensions.add("https://example.com/required-extension");
        ServerCallContext contextWithExtension = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                requestedExtensions
        );

        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, contextWithExtension);

        // Should succeed without ExtensionSupportRequiredError
        assertNull(response.getError());
        Assertions.assertSame(message, response.getResult());
    }

    @Test
    public void testVersionNotSupportedErrorOnMessageSend() {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(agentCard, requestHandler, internalExecutor);

        // Create context with incompatible version 2.0
        ServerCallContext contextWithVersion = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                new HashSet<>(),
                "2.0"  // Incompatible version
        );

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, contextWithVersion);

        assertInstanceOf(VersionNotSupportedError.class, response.getError());
        Assertions.assertTrue(response.getError().getMessage().contains("2.0"));
        assertNull(response.getResult());
    }

    @Test
    public void testVersionNotSupportedErrorOnMessageSendStream() throws Exception {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(agentCard, requestHandler, internalExecutor);

        // Create context with incompatible version 2.0
        ServerCallContext contextWithVersion = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                new HashSet<>(),
                "2.0"  // Incompatible version
        );

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, contextWithVersion);

        List<SendStreamingMessageResponse> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        response.subscribe(new Flow.Subscriber<SendStreamingMessageResponse>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse item) {
                results.add(item);
                subscription.request(1);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        // Wait for async processing
        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected to receive error event within timeout");

        assertEquals(1, results.size());
        SendStreamingMessageResponse result = results.get(0);
        assertInstanceOf(VersionNotSupportedError.class, result.getError());
        Assertions.assertTrue(result.getError().getMessage().contains("2.0"));
        assertNull(result.getResult());
    }

    @Test
    public void testCompatibleVersionSuccess() {
        // Create AgentCard with protocol version 1.0
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(agentCard, requestHandler, internalExecutor);

        // Create context with compatible version 1.1
        ServerCallContext contextWithVersion = new ServerCallContext(
                UnauthenticatedUser.INSTANCE,
                Map.of("foo", "bar"),
                new HashSet<>(),
                "1.1"  // Compatible version (same major version)
        );

        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, contextWithVersion);

        // Should succeed without error
        assertNull(response.getError());
        Assertions.assertSame(message, response.getResult());
    }

    @Test
    public void testNoVersionDefaultsToCurrentVersionSuccess() {
        // Create AgentCard with protocol version 1.0 (current version)
        AgentCard agentCard = AgentCard.builder()
                .name("test-card")
                .description("Test card with version 1.0")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("JSONRPC", "http://localhost:9999")))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(agentCard, requestHandler, internalExecutor);

        // Use default callContext (no version - should default to 1.0)
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.id())
                .contextId(MINIMAL_TASK.contextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);

        // Should succeed without error (defaults to 1.0)
        assertNull(response.getError());
        Assertions.assertSame(message, response.getResult());
    }

    @Test
    public void testListTasksEmptyResultIncludesAllFields() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        // Query for a context that doesn't exist - should return empty result
        ListTasksParams params = ListTasksParams.builder()
                .contextId("nonexistent-context-id")
                .tenant("")
                .build();
        ListTasksRequest request = new ListTasksRequest("1", params);
        ListTasksResponse response = handler.onListTasks(request, callContext);

        // Should return success with all fields present (not null)
        assertNull(response.getError());
        ListTasksResult result = response.getResult();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.tasks(), "tasks field should not be null");
        assertEquals(0, result.tasks().size(), "tasks should be empty list");
        assertEquals(0, result.totalSize(), "totalSize should be 0");
        assertEquals(0, result.pageSize(), "pageSize should be 0");
        // nextPageToken can be null for empty results
    }
}
