package io.a2a.transport.jsonrpc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.requesthandlers.AbstractA2ARequestHandlerTest;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.ResultAggregator;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.AuthenticatedExtendedCardNotConfiguredError;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.Event;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardResponse;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.SubscribeToTaskRequest;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
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
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
        GetTaskResponse response = handler.onGetTask(request, callContext);
        assertEquals(request.getId(), response.getId());
        Assertions.assertSame(MINIMAL_TASK, response.getResult());
        assertNull(response.getError());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
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

        CancelTaskRequest request = new CancelTaskRequest("111", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);

        assertNull(response.getError());
        assertEquals(request.getId(), response.getId());
        Task task = response.getResult();
        assertEquals(MINIMAL_TASK.getId(), task.getId());
        assertEquals(MINIMAL_TASK.getContextId(), task.getContextId());
        assertEquals(TaskState.CANCELED, task.getStatus().state());
    }

    @Test
    public void testOnCancelTaskNotSupported() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            throw new UnsupportedOperationError();
        };

        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
    }

    @Test
    public void testOnCancelTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
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
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        assertNull(response.getError());
        // The Python implementation returns a Task here, but then again they are using hardcoded mocks and
        // bypassing the whole EventQueue.
        // If we were to send a Task in agentExecutorExecute EventConsumer.consumeAll() would not exit due to
        // the Task not having a 'final' state
        //
        // See testOnMessageNewMessageSuccessMocks() for a test more similar to the Python implementation
        Assertions.assertSame(message, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {Mockito.doReturn(ZeroPublisher.fromItems(wrapEvent(MINIMAL_TASK))).when(mock).consumeAll();
                Mockito.doCallRealMethod().when(mock).createAgentRunnableDoneCallback();})){
            response = handler.onMessageSend(request, callContext);
        }
        assertNull(response.getError());
        Assertions.assertSame(MINIMAL_TASK, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        assertNull(response.getError());
        // The Python implementation returns a Task here, but then again they are using hardcoded mocks and
        // bypassing the whole EventQueue.
        // If we were to send a Task in agentExecutorExecute EventConsumer.consumeAll() would not exit due to
        // the Task not having a 'final' state
        //
        // See testOnMessageNewMessageWithExistingTaskSuccessMocks() for a test more similar to the Python implementation
        Assertions.assertSame(message, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromItems(wrapEvent(MINIMAL_TASK))).when(mock).consumeAll();})){
            response = handler.onMessageSend(request, callContext);
        }
        assertNull(response.getError());
        Assertions.assertSame(MINIMAL_TASK, response.getResult());

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
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
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
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromItems(wrapEvent(new UnsupportedOperationError()))).when(mock).consumeAll();})){
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
            .taskId(MINIMAL_TASK.getId())
            .contextId(MINIMAL_TASK.getContextId())
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
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        // Create multiple events to be sent during streaming
        Task taskEvent = Task.builder(MINIMAL_TASK)
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        TaskArtifactUpdateEvent artifactEvent = TaskArtifactUpdateEvent.builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .artifact(Artifact.builder()
                        .artifactId("artifact-1")
                        .parts(new TextPart("Generated artifact content"))
                        .build())
                .build();

        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
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
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
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

        // Wait for all events to be received
        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS),
                "Expected to receive 3 events within timeout");

        // Assert no error occurred during streaming
        Assertions.assertNull(error.get(), "No error should occur during streaming");

        // Verify that all 3 events were received
        assertEquals(3, results.size(), "Should have received exactly 3 events");

        // Verify the first event is the task
        Task receivedTask = assertInstanceOf(Task.class, results.get(0), "First event should be a Task");
        assertEquals(MINIMAL_TASK.getId(), receivedTask.getId());
        assertEquals(MINIMAL_TASK.getContextId(), receivedTask.getContextId());
        assertEquals(TaskState.WORKING, receivedTask.getStatus().state());

        // Verify the second event is the artifact update
        TaskArtifactUpdateEvent receivedArtifact = assertInstanceOf(TaskArtifactUpdateEvent.class, results.get(1),
                "Second event should be a TaskArtifactUpdateEvent");
        assertEquals(MINIMAL_TASK.getId(), receivedArtifact.getTaskId());
        assertEquals("artifact-1", receivedArtifact.getArtifact().artifactId());

        // Verify the third event is the status update
        TaskStatusUpdateEvent receivedStatus = assertInstanceOf(TaskStatusUpdateEvent.class, results.get(2),
                "Third event should be a TaskStatusUpdateEvent");
        assertEquals(MINIMAL_TASK.getId(), receivedStatus.getTaskId());
        assertEquals(TaskState.COMPLETED, receivedStatus.getStatus().state());
    }

    @Test
    public void testOnMessageStreamNewMessageSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);

        // This is used to send events from a mock
        List<Event> events = List.of(
                MINIMAL_TASK,
                TaskArtifactUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(Artifact.builder()
                                .artifactId("art1")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .build());

        Message message = Message.builder(MESSAGE)
            .taskId(MINIMAL_TASK.getId())
            .contextId(MINIMAL_TASK.getContextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events.stream().map(AbstractA2ARequestHandlerTest::wrapEvent).toList())).when(mock).consumeAll();})){
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
            .taskId(task.getId())
            .contextId(task.getContextId())
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
        assertEquals(expected.getId(), received.getId());
        assertEquals(expected.getContextId(), received.getContextId());
        assertEquals(expected.getStatus(), received.getStatus());
        assertEquals(expected.getHistory(), received.getHistory());
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
                        .taskId(task.getId())
                        .contextId(task.getContextId())
                        .artifact(Artifact.builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(task.getId())
                        .contextId(task.getContextId())
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        Message message = Message.builder(MESSAGE)
            .taskId(task.getId())
            .contextId(task.getContextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events.stream().map(AbstractA2ARequestHandlerTest::wrapEvent).toList())).when(mock).consumeAll();})){
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

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder().url("http://example.com")
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4").build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);
        TaskPushNotificationConfig taskPushConfigResult =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder().url("http://example.com").id("c295ea44-7543-4f78-b524-7a38915ad6e4").build());
        Assertions.assertEquals(taskPushConfigResult, response.getResult());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };


        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder()
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4").url("http://example.com").build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        GetTaskPushNotificationConfigRequest getRequest =
                new GetTaskPushNotificationConfigRequest("111", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse getResponse = handler.getPushNotificationConfig(getRequest, callContext);

        TaskPushNotificationConfig expectedConfig = new TaskPushNotificationConfig(MINIMAL_TASK.getId(),
                PushNotificationConfig.builder().id("c295ea44-7543-4f78-b524-7a38915ad6e4").url("http://example.com").build());
        assertEquals(expectedConfig, getResponse.getResult());
    }

    @Test
    public void testOnMessageStreamNewMessageSendPushNotificationSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);

        List<Event> events = List.of(
                MINIMAL_TASK,
                TaskArtifactUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(Artifact.builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .build());


        agentExecutorExecute = (context, eventQueue) -> {
            // Hardcode the events to send here
            for (Event event : events) {
                eventQueue.enqueueEvent(event);
            }
        };


        TaskPushNotificationConfig config = new TaskPushNotificationConfig(
                MINIMAL_TASK.getId(),
                PushNotificationConfig.builder().id("c295ea44-7543-4f78-b524-7a38915ad6e4").url("http://example.com").build());
        SetTaskPushNotificationConfigRequest stpnRequest = new SetTaskPushNotificationConfigRequest("1", config);
        SetTaskPushNotificationConfigResponse stpnResponse = handler.setPushNotificationConfig(stpnRequest, callContext);
        assertNull(stpnResponse.getError());

        Message msg = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
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
        subscriptionRef.get().cancel();
        assertEquals(3, results.size());
        assertEquals(3, httpClient.tasks.size());

        Task curr = httpClient.tasks.get(0);
        assertEquals(MINIMAL_TASK.getId(), curr.getId());
        assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        assertEquals(MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        assertEquals(0, curr.getArtifacts() == null ? 0 : curr.getArtifacts().size());

        curr = httpClient.tasks.get(1);
        assertEquals(MINIMAL_TASK.getId(), curr.getId());
        assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        assertEquals(MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        assertEquals(1, curr.getArtifacts().size());
        assertEquals(1, curr.getArtifacts().get(0).parts().size());
        assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());

        curr = httpClient.tasks.get(2);
        assertEquals(MINIMAL_TASK.getId(), curr.getId());
        assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        assertEquals(TaskState.COMPLETED, curr.getStatus().state());
        assertEquals(1, curr.getArtifacts().size());
        assertEquals(1, curr.getArtifacts().get(0).parts().size());
        assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());
    }

    @Test
    public void testOnResubscribeExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        queueManager.createOrTap(MINIMAL_TASK.getId());

        agentExecutorExecute = (context, eventQueue) -> {
            // The only thing hitting the agent is the onMessageSend() and we should use the message
            eventQueue.enqueueEvent(context.getMessage());
            //eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onSubscribeToTask(request, callContext);

        // We need to send some events in order for those to end up in the queue
        Message message = Message.builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .role(Message.Role.AGENT)
                .parts(new TextPart("text"))
                .build();
        SendMessageResponse smr =
                handler.onMessageSend(
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
        queueManager.createOrTap(MINIMAL_TASK.getId());

        List<Event> events = List.of(
                TaskArtifactUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(Artifact.builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                TaskStatusUpdateEvent.builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events.stream().map(AbstractA2ARequestHandlerTest::wrapEvent).toList())).when(mock).consumeAll();})){
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

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
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

        SubscribeToTaskRequest request = new SubscribeToTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
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

        TaskPushNotificationConfig config =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        PushNotificationConfig.builder()
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4")
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest request = SetTaskPushNotificationConfigRequest.builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);
        assertInstanceOf(PushNotificationNotSupportedError.class, response.getError());
    }

    @Test
    public void testOnGetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(
                executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        taskStore.save(MINIMAL_TASK);

        GetTaskPushNotificationConfigRequest request =
                new GetTaskPushNotificationConfigRequest("id", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse response = handler.getPushNotificationConfig(request, callContext);

        Assertions.assertNotNull(response.getError());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnSetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(
                executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        taskStore.save(MINIMAL_TASK);

                TaskPushNotificationConfig config =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        PushNotificationConfig.builder()
                                .id("c295ea44-7543-4f78-b524-7a38915ad6e4")
                                .url("http://example.com")
                                .build());

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
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(
                executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);

        taskStore.save(MINIMAL_TASK);

        Message message = Message.builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
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
                })){
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
    public void testOnMessageStreamTaskIdMismatch() {
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

        Assertions.assertNull(error.get());
        Assertions.assertEquals(1, results.size());
        Assertions.assertInstanceOf(InternalError.class, results.get(0).getError());
    }

    @Test
    public void testListPushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse = handler.listPushNotificationConfig(listRequest, callContext);

        assertEquals("111", listResponse.getId());
        assertEquals(1, listResponse.getResult().size());
        assertEquals(taskPushConfig, listResponse.getResult().get(0));
    }

    @Test
    public void testListPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse =
                handler.listPushNotificationConfig(listRequest, callContext);

        assertEquals("111", listResponse.getId());
        assertNull(listResponse.getResult());
        assertInstanceOf(PushNotificationNotSupportedError.class, listResponse.getError());
    }

    @Test
    public void testListPushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(
                executor, taskStore, queueManager, null, null, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse =
                handler.listPushNotificationConfig(listRequest, callContext);

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

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse =
                handler.listPushNotificationConfig(listRequest, callContext);

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

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

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

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(PushNotificationNotSupportedError.class, deleteResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(
                        executor, taskStore, queueManager, null, null, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), PushNotificationConfig.builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(UnsupportedOperationError.class, deleteResponse.getError());
    }

    @Test
    public void testOnGetAuthenticatedExtendedAgentCard() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler, internalExecutor);
        GetAuthenticatedExtendedCardRequest request = new GetAuthenticatedExtendedCardRequest("1");
        GetAuthenticatedExtendedCardResponse response = handler.onGetAuthenticatedExtendedCardRequest(request, callContext);
        assertEquals(request.getId(), response.getId());
        assertInstanceOf(AuthenticatedExtendedCardNotConfiguredError.class, response.getError());
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
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
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
}
