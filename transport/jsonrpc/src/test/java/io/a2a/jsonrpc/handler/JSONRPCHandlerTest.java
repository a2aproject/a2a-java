package io.a2a.jsonrpc.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
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
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.Event;
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
import io.a2a.spec.TaskResubscriptionRequest;
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

    private final ServerCallContext callContext = new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"));

    @Test
    public void testOnGetTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
        GetTaskResponse response = handler.onGetTask(request, callContext);
        Assertions.assertEquals(request.getId(), response.getId());
        Assertions.assertSame(MINIMAL_TASK, response.getResult());
        Assertions.assertNull(response.getError());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
        GetTaskResponse response = handler.onGetTask(request, callContext);
        Assertions.assertEquals(request.getId(), response.getId());
        Assertions.assertInstanceOf(TaskNotFoundError.class, response.getError());
        Assertions.assertNull(response.getResult());
    }

    @Test
    public void testOnCancelTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
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

        Assertions.assertNull(response.getError());
        Assertions.assertEquals(request.getId(), response.getId());
        Task task = response.getResult();
        Assertions.assertEquals(MINIMAL_TASK.getId(), task.getId());
        Assertions.assertEquals(MINIMAL_TASK.getContextId(), task.getContextId());
        Assertions.assertEquals(TaskState.CANCELED, task.getStatus().state());
    }

    @Test
    public void testOnCancelTaskNotSupported() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            throw new UnsupportedOperationError();
        };

        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);
        Assertions.assertEquals(request.getId(), response.getId());
        Assertions.assertNull(response.getResult());
        Assertions.assertInstanceOf(UnsupportedOperationError.class, response.getError());
    }

    @Test
    public void testOnCancelTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);
        Assertions.assertEquals(request.getId(), response.getId());
        Assertions.assertNull(response.getResult());
        Assertions.assertInstanceOf(TaskNotFoundError.class, response.getError());
    }

    @Test
    public void testOnMessageNewMessageSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        Assertions.assertNull(response.getError());
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
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {Mockito.doReturn(ZeroPublisher.fromItems(MINIMAL_TASK)).when(mock).consumeAll();})){
            response = handler.onMessageSend(request, callContext);
        }
        Assertions.assertNull(response.getError());
        Assertions.assertSame(MINIMAL_TASK, response.getResult());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        Assertions.assertNull(response.getError());
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
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromItems(MINIMAL_TASK)).when(mock).consumeAll();})){
            response = handler.onMessageSend(request, callContext);
        }
        Assertions.assertNull(response.getError());
        Assertions.assertSame(MINIMAL_TASK, response.getResult());

    }

    @Test
    public void testOnMessageError() {
        // See testMessageOnErrorMocks() for a test more similar to the Python implementation, using mocks for
        // EventConsumer.consumeAll()
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(new UnsupportedOperationError());
        };
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        Assertions.assertInstanceOf(UnsupportedOperationError.class, response.getError());
        Assertions.assertNull(response.getResult());
    }

    @Test
    public void testOnMessageErrorMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromItems(new UnsupportedOperationError())).when(mock).consumeAll();})){
            response = handler.onMessageSend(request, callContext);
        }

        Assertions.assertInstanceOf(UnsupportedOperationError.class, response.getError());
        Assertions.assertNull(response.getResult());
    }

    @Test
    public void testOnMessageStreamNewMessageSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        Message message = new Message.Builder(MESSAGE)
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

        // The Python implementation has several events emitted since it uses mocks. Also, in the
        // implementation, a Message is considered a 'final' Event in EventConsumer.consumeAll()
        // so there would be no more Events.
        //
        // See testOnMessageStreamNewMessageSuccessMocks() for a test more similar to the Python implementation
        Assertions.assertEquals(1, results.size());
        Assertions.assertSame(message, results.get(0));
    }

    @Test
    public void testOnMessageStreamNewMessageSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        // This is used to send events from a mock
        List<Event> events = List.of(
                MINIMAL_TASK,
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("art1")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .status(new TaskStatus(TaskState.COMPLETED))
                        .build());

        Message message = new Message.Builder(MESSAGE)
            .taskId(MINIMAL_TASK.getId())
            .contextId(MINIMAL_TASK.getContextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            response = handler.onMessageSendStream(request, callContext);
        }

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

            }

            @Override
            public void onComplete() {

            }
        });

        Assertions.assertEquals(events, results);
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        Task task = new Task.Builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(task);

        Message message = new Message.Builder(MESSAGE)
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
        Task expected = new Task.Builder(task)
                .history(message)
                .build();
        Assertions.assertEquals(1, results.size());
        StreamingEventKind receivedType = results.get(0);
        Assertions.assertInstanceOf(Task.class, receivedType);
        Task received = (Task) receivedType;
        Assertions.assertEquals(expected.getId(), received.getId());
        Assertions.assertEquals(expected.getContextId(), received.getContextId());
        Assertions.assertEquals(expected.getStatus(), received.getStatus());
        Assertions.assertEquals(expected.getHistory(), received.getHistory());
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccessMocks() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        Task task = new Task.Builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(task);

        // This is used to send events from a mock
        List<Event> events = List.of(
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(task.getId())
                        .contextId(task.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId(task.getId())
                        .contextId(task.getContextId())
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        Message message = new Message.Builder(MESSAGE)
            .taskId(task.getId())
            .contextId(task.getContextId())
            .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            response = handler.onMessageSendStream(request, callContext);
        }

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

            }

            @Override
            public void onComplete() {

            }
        });

        Assertions.assertEquals(events, results);
    }


    @Test
    public void testSetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);
        Assertions.assertSame(taskPushConfig, response.getResult());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };


        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        GetTaskPushNotificationConfigRequest getRequest =
                new GetTaskPushNotificationConfigRequest("111", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse getResponse = handler.getPushNotificationConfig(getRequest, callContext);

        TaskPushNotificationConfig expectedConfig = new TaskPushNotificationConfig(MINIMAL_TASK.getId(),
                new PushNotificationConfig.Builder().id(MINIMAL_TASK.getId()).url("http://example.com").build());
        Assertions.assertEquals(expectedConfig, getResponse.getResult());
    }

    @Test
    public void testOnMessageStreamNewMessageSendPushNotificationSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        List<Event> events = List.of(
                MINIMAL_TASK,
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
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
                new PushNotificationConfig.Builder().url("http://example.com").build());
        SetTaskPushNotificationConfigRequest stpnRequest = new SetTaskPushNotificationConfigRequest("1", config);
        SetTaskPushNotificationConfigResponse stpnResponse = handler.setPushNotificationConfig(stpnRequest, callContext);
        Assertions.assertNull(stpnResponse.getError());

        Message msg = new Message.Builder(MESSAGE)
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
        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals(3, httpClient.tasks.size());

        Task curr = httpClient.tasks.get(0);
        Assertions.assertEquals(MINIMAL_TASK.getId(), curr.getId());
        Assertions.assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        Assertions.assertEquals(MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        Assertions.assertEquals(0, curr.getArtifacts() == null ? 0 : curr.getArtifacts().size());

        curr = httpClient.tasks.get(1);
        Assertions.assertEquals(MINIMAL_TASK.getId(), curr.getId());
        Assertions.assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        Assertions.assertEquals(MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        Assertions.assertEquals(1, curr.getArtifacts().size());
        Assertions.assertEquals(1, curr.getArtifacts().get(0).parts().size());
        Assertions.assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());

        curr = httpClient.tasks.get(2);
        Assertions.assertEquals(MINIMAL_TASK.getId(), curr.getId());
        Assertions.assertEquals(MINIMAL_TASK.getContextId(), curr.getContextId());
        Assertions.assertEquals(TaskState.COMPLETED, curr.getStatus().state());
        Assertions.assertEquals(1, curr.getArtifacts().size());
        Assertions.assertEquals(1, curr.getArtifacts().get(0).parts().size());
        Assertions.assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());
    }

    @Test
    public void testOnResubscribeExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        queueManager.createOrTap(MINIMAL_TASK.getId());

        agentExecutorExecute = (context, eventQueue) -> {
            // The only thing hitting the agent is the onMessageSend() and we should use the message
            eventQueue.enqueueEvent(context.getMessage());
            //eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onResubscribeToTask(request, callContext);

        // We need to send some events in order for those to end up in the queue
        Message message = new Message.Builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .role(Message.Role.AGENT)
                .parts(new TextPart("text"))
                .build();
        SendMessageResponse smr =
                handler.onMessageSend(
                        new SendMessageRequest("1", new MessageSendParams(message, null, null)),
                        callContext);
        Assertions.assertNull(smr.getError());


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
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        Assertions.assertEquals(1, results.size());
    }


    @Test
    public void testOnResubscribeExistingTaskSuccessMocks() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        queueManager.createOrTap(MINIMAL_TASK.getId());

        List<Event> events = List.of(
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .status(new TaskStatus(TaskState.WORKING))
                        .build());

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            response = handler.onResubscribeToTask(request, callContext);
        }

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
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });

        // The Python implementation has several events emitted since it uses mocks.
        //
        // See testOnMessageStreamNewMessageExistingTaskSuccessMocks() for a test more similar to the Python implementation
        Assertions.assertEquals(events, results);
    }

    @Test
    public void testOnResubscribeNoExistingTaskError() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onResubscribeToTask(request, callContext);

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

        Assertions.assertEquals(1, results.size());
        Assertions.assertNull(results.get(0).getResult());
        Assertions.assertInstanceOf(TaskNotFoundError.class, results.get(0).getError());
    }

    @Test
    public void testStreamingNotSupportedError() {
        AgentCard card = createAgentCard(false, true, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        SendStreamingMessageRequest request = new SendStreamingMessageRequest.Builder()
                .id("1")
                .params(new MessageSendParams.Builder()
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

        Assertions.assertEquals(1, results.size());
        if (results.get(0).getError() != null && results.get(0).getError() instanceof InvalidRequestError ire) {
            Assertions.assertEquals("Streaming is not supported by the agent", ire.getMessage());
        } else {
            Assertions.fail("Expected a response containing an error");
        }
    }

    @Test
    public void testStreamingNotSupportedErrorOnResubscribeToTask() {
        // This test does not exist in the Python implementation
        AgentCard card = createAgentCard(false, true, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onResubscribeToTask(request, callContext);

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

        Assertions.assertEquals(1, results.size());
        if (results.get(0).getError() != null && results.get(0).getError() instanceof InvalidRequestError ire) {
            Assertions.assertEquals("Streaming is not supported by the agent", ire.getMessage());
        } else {
            Assertions.fail("Expected a response containing an error");
        }
    }


    @Test
    public void testPushNotificationsNotSupportedError() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);
        taskStore.save(MINIMAL_TASK);

        TaskPushNotificationConfig config =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest.Builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);
        Assertions.assertInstanceOf(PushNotificationNotSupportedError.class, response.getError());
    }

    @Test
    public void testOnGetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        taskStore.save(MINIMAL_TASK);

        GetTaskPushNotificationConfigRequest request =
                new GetTaskPushNotificationConfigRequest("id", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse response = handler.getPushNotificationConfig(request, callContext);

        Assertions.assertNotNull(response.getError());
        Assertions.assertInstanceOf(UnsupportedOperationError.class, response.getError());
        Assertions.assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnSetPushNotificationNoPushNotifierConfig() {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        taskStore.save(MINIMAL_TASK);

                TaskPushNotificationConfig config =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest.Builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);

        Assertions.assertInstanceOf(UnsupportedOperationError.class, response.getError());
        Assertions.assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnMessageSendInternalError() {
        DefaultRequestHandler mocked = Mockito.mock(DefaultRequestHandler.class);
        Mockito.doThrow(new InternalError("Internal Error")).when(mocked)
                .onMessageSend(Mockito.any(MessageSendParams.class), Mockito.any(ServerCallContext.class));

        JSONRPCHandler handler = new JSONRPCHandler(CARD, mocked);

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);

        Assertions.assertInstanceOf(InternalError.class, response.getError());
    }

    @Test
    public void testOnMessageStreamInternalError() {
        DefaultRequestHandler mocked = Mockito.mock(DefaultRequestHandler.class);
        Mockito.doThrow(new InternalError("Internal Error")).when(mocked)
                .onMessageSendStream(Mockito.any(MessageSendParams.class), Mockito.any(ServerCallContext.class));

        JSONRPCHandler handler = new JSONRPCHandler(CARD, mocked);

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

        Assertions.assertEquals(1, results.size());
        Assertions.assertInstanceOf(InternalError.class, results.get(0).getError());
    }

    @Test
    @Disabled
    public void testDefaultRequestHandlerWithCustomComponents() {
        // Not much happening in the Python test beyond checking that the DefaultRequestHandler
        // constructor sets the fields as expected
    }

    @Test
    public void testOnMessageSendErrorHandling() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);

        taskStore.save(MINIMAL_TASK);

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response;

        try (MockedConstruction<ResultAggregator> mocked = Mockito.mockConstruction(
                ResultAggregator.class,
                (mock, context) ->
                        Mockito.doThrow(
                                new UnsupportedOperationError())
                                .when(mock).consumeAndBreakOnInterrupt(Mockito.any(EventConsumer.class)))){
            response = handler.onMessageSend(request, callContext);
        }

        Assertions.assertInstanceOf(UnsupportedOperationError.class, response.getError());

    }

    @Test
    public void testOnMessageSendTaskIdMismatch() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorExecute = ((context, eventQueue) -> {
            eventQueue.enqueueEvent(MINIMAL_TASK);
        });
        SendMessageRequest request = new SendMessageRequest("1",
                new MessageSendParams(MESSAGE, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);
        Assertions.assertInstanceOf(InternalError.class, response.getError());

    }

    @Test
    public void testOnMessageStreamTaskIdMismatch() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);

        agentExecutorExecute = ((context, eventQueue) -> {
            eventQueue.enqueueEvent(MINIMAL_TASK);
        });

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

        Assertions.assertNull(error.get());
        Assertions.assertEquals(1, results.size());
        Assertions.assertInstanceOf(InternalError.class, results.get(0).getError());
    }

    @Test
    public void testListPushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse = handler.listPushNotificationConfig(listRequest, callContext);

        Assertions.assertEquals("111", listResponse.getId());
        Assertions.assertEquals(1, listResponse.getResult().size());
        Assertions.assertEquals(taskPushConfig, listResponse.getResult().get(0));
    }

    @Test
    public void testListPushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse =
                handler.listPushNotificationConfig(listRequest, callContext);

        Assertions.assertEquals("111", listResponse.getId());
        Assertions.assertNull(listResponse.getResult());
        Assertions.assertInstanceOf(PushNotificationNotSupportedError.class, listResponse.getError());
    }

    @Test
    public void testListPushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse =
                handler.listPushNotificationConfig(listRequest, callContext);

        Assertions.assertEquals("111", listResponse.getId());
        Assertions.assertNull(listResponse.getResult());
        Assertions.assertInstanceOf(UnsupportedOperationError.class, listResponse.getError());
    }

    @Test
    public void testListPushNotificationConfigTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest listRequest =
                new ListTaskPushNotificationConfigRequest("111", new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        ListTaskPushNotificationConfigResponse listResponse =
                handler.listPushNotificationConfig(listRequest, callContext);

        Assertions.assertEquals("111", listResponse.getId());
        Assertions.assertNull(listResponse.getResult());
        Assertions.assertInstanceOf(TaskNotFoundError.class, listResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        Assertions.assertEquals("111", deleteResponse.getId());
        Assertions.assertNull(deleteResponse.getError());
        Assertions.assertNull(deleteResponse.getResult());
    }

    @Test
    public void testDeletePushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, true);
        JSONRPCHandler handler = new JSONRPCHandler(card, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        Assertions.assertEquals("111", deleteResponse.getId());
        Assertions.assertNull(deleteResponse.getResult());
        Assertions.assertInstanceOf(PushNotificationNotSupportedError.class, deleteResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        JSONRPCHandler handler = new JSONRPCHandler(CARD, requestHandler);
        taskStore.save(MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(), new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id(MINIMAL_TASK.getId())
                        .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest deleteRequest =
                new DeleteTaskPushNotificationConfigRequest("111", new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        Assertions.assertEquals("111", deleteResponse.getId());
        Assertions.assertNull(deleteResponse.getResult());
        Assertions.assertInstanceOf(UnsupportedOperationError.class, deleteResponse.getError());
    }

}
