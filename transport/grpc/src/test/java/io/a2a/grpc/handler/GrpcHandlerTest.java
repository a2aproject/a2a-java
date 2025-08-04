package io.a2a.grpc.handler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Empty;
import com.google.protobuf.Struct;

import io.a2a.grpc.AuthenticationInfo;
import io.a2a.grpc.CancelTaskRequest;
import io.a2a.grpc.CreateTaskPushNotificationConfigRequest;
import io.a2a.grpc.DeleteTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskRequest;
import io.a2a.grpc.ListTaskPushNotificationConfigRequest;
import io.a2a.grpc.ListTaskPushNotificationConfigResponse;
import io.a2a.grpc.Message;
import io.a2a.grpc.Part;
import io.a2a.grpc.PushNotificationConfig;
import io.a2a.grpc.Role;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.Task;
import io.a2a.grpc.TaskPushNotificationConfig;
import io.a2a.grpc.TaskState;
import io.a2a.grpc.TaskStatus;
import io.a2a.grpc.TaskSubscriptionRequest;
import io.a2a.server.ServerCallContext;
import io.a2a.server.events.EventConsumer;
import io.a2a.server.requesthandlers.AbstractA2ARequestHandlerTest;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.InternalError;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.testing.StreamRecorder;
import io.grpc.stub.StreamObserver;
import mutiny.zero.ZeroPublisher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class GrpcHandlerTest extends AbstractA2ARequestHandlerTest {

    private static final Message GRPC_MESSAGE = Message.newBuilder()
            .setTaskId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
            .setContextId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId())
            .setMessageId(AbstractA2ARequestHandlerTest.MESSAGE.getMessageId())
            .setRole(Role.ROLE_AGENT)
            .addContent(Part.newBuilder().setText(((TextPart) AbstractA2ARequestHandlerTest.MESSAGE.getParts().get(0)).getText()).build())
            .setMetadata(Struct.newBuilder().build())
            .build();


    @Test
    public void testOnGetTaskSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        GetTaskRequest request = GetTaskRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();

        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.getTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        Assertions.assertNull(streamRecorder.getError());
        List<Task> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Task task = result.get(0);
        assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId(), task.getId());
        assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId(), task.getContextId());
        assertEquals(TaskState.TASK_STATE_SUBMITTED, task.getStatus().getState());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        GetTaskRequest request = GetTaskRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();

        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.getTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertGrpcError(streamRecorder, Status.Code.NOT_FOUND);
    }

    @Test
    public void testOnCancelTaskSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            // We need to cancel the task or the EventConsumer never finds a 'final' event.
            // Looking at the Python implementation, they typically use AgentExecutors that
            // don't support cancellation. So my theory is the Agent updates the task to the CANCEL status
            io.a2a.spec.Task task = context.getTask();
            TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
            taskUpdater.cancel();
        };

        CancelTaskRequest request = CancelTaskRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.cancelTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        Assertions.assertNull(streamRecorder.getError());
        List<Task> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Task task = result.get(0);
        assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId(), task.getId());
        assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId(), task.getContextId());
        assertEquals(TaskState.TASK_STATE_CANCELLED, task.getStatus().getState());
    }

    @Test
    public void testOnCancelTaskNotSupported() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);

        agentExecutorCancel = (context, eventQueue) -> {
            throw new UnsupportedOperationError();
        };

        CancelTaskRequest request = CancelTaskRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.cancelTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testOnCancelTaskNotFound() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        CancelTaskRequest request = CancelTaskRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.cancelTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertGrpcError(streamRecorder, Status.Code.NOT_FOUND);
    }

    @Test
    public void testOnMessageNewMessageSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };

        StreamRecorder<SendMessageResponse> streamRecorder = sendMessageRequest(handler);
        Assertions.assertNull(streamRecorder.getError());
        List<SendMessageResponse> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        SendMessageResponse response = result.get(0);
        assertEquals(GRPC_MESSAGE, response.getMsg());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };
        StreamRecorder<SendMessageResponse> streamRecorder = sendMessageRequest(handler);
        Assertions.assertNull(streamRecorder.getError());
        List<SendMessageResponse> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        SendMessageResponse response = result.get(0);
        assertEquals(GRPC_MESSAGE, response.getMsg());
    }

    @Test
    public void testOnMessageError() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(new UnsupportedOperationError());
        };
        StreamRecorder<SendMessageResponse> streamRecorder = sendMessageRequest(handler);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testSetPushNotificationConfigSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + "config456";
        StreamRecorder<TaskPushNotificationConfig> streamRecorder = createTaskPushNotificationConfigRequest(handler, NAME);

        Assertions.assertNull(streamRecorder.getError());
        List<TaskPushNotificationConfig> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        TaskPushNotificationConfig response = result.get(0);
        assertEquals(NAME, response.getName());
        PushNotificationConfig responseConfig = response.getPushNotificationConfig();
        assertEquals("config456", responseConfig.getId());
        assertEquals("http://example.com", responseConfig.getUrl());
        assertEquals(AuthenticationInfo.getDefaultInstance(), responseConfig.getAuthentication());
        Assertions.assertTrue(responseConfig.getToken().isEmpty());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + "config456";

        // first set the task push notification config
        StreamRecorder<TaskPushNotificationConfig> streamRecorder = createTaskPushNotificationConfigRequest(handler, NAME);
        Assertions.assertNull(streamRecorder.getError());

        // then get the task push notification config
        streamRecorder = getTaskPushNotificationConfigRequest(handler, NAME);
        Assertions.assertNull(streamRecorder.getError());
        List<TaskPushNotificationConfig> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        TaskPushNotificationConfig response = result.get(0);
        assertEquals(NAME, response.getName());
        PushNotificationConfig responseConfig = response.getPushNotificationConfig();
        assertEquals("config456", responseConfig.getId());
        assertEquals("http://example.com", responseConfig.getUrl());
        assertEquals(AuthenticationInfo.getDefaultInstance(), responseConfig.getAuthentication());
        Assertions.assertTrue(responseConfig.getToken().isEmpty());
    }

    @Test
    public void testPushNotificationsNotSupportedError() throws Exception {
        AgentCard card = AbstractA2ARequestHandlerTest.createAgentCard(true, false, true);
        GrpcHandler handler = new GrpcHandler(card, requestHandler);
        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        StreamRecorder<TaskPushNotificationConfig> streamRecorder = createTaskPushNotificationConfigRequest(handler, NAME);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testOnGetPushNotificationNoPushNotifierConfig() throws Exception {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = AbstractA2ARequestHandlerTest.createAgentCard(false, true, false);
        GrpcHandler handler = new GrpcHandler(card, requestHandler);
        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        StreamRecorder<TaskPushNotificationConfig> streamRecorder = getTaskPushNotificationConfigRequest(handler, NAME);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testOnSetPushNotificationNoPushNotifierConfig() throws Exception {
        // Create request handler without a push notifier
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        AgentCard card = AbstractA2ARequestHandlerTest.createAgentCard(false, true, false);
        GrpcHandler handler = new GrpcHandler(card, requestHandler);
        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        StreamRecorder<TaskPushNotificationConfig> streamRecorder = createTaskPushNotificationConfigRequest(handler, NAME);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testOnMessageStreamNewMessageSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        StreamRecorder<StreamResponse> streamRecorder = sendStreamingMessageRequest(handler);
        Assertions.assertNull(streamRecorder.getError());
        List<StreamResponse> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        StreamResponse response = result.get(0);
        Assertions.assertTrue(response.hasMsg());
        Message message = response.getMsg();
        Assertions.assertEquals(GRPC_MESSAGE, message);
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        io.a2a.spec.Task task = new io.a2a.spec.Task.Builder(AbstractA2ARequestHandlerTest.MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(task);

        List<StreamResponse> results = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        httpClient.latch = latch;
        StreamObserver<StreamResponse> streamObserver = new StreamObserver<>() {
            @Override
            public void onNext(StreamResponse streamResponse) {
                results.add(streamResponse);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errors.add(throwable);
            }

            @Override
            public void onCompleted() {
            }
        };
        sendStreamingMessageRequest(handler, streamObserver);
        Assertions.assertTrue(latch.await(1, TimeUnit.SECONDS));
        Assertions.assertTrue(errors.isEmpty());
        Assertions.assertEquals(1, results.size());
        StreamResponse response = results.get(0);
        Assertions.assertTrue(response.hasTask());
        Task taskResponse = response.getTask();

        Task expected = Task.newBuilder()
                .setId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .setContextId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId())
                .addAllHistory(List.of(GRPC_MESSAGE))
                .setStatus(TaskStatus.newBuilder().setStateValue(TaskState.TASK_STATE_SUBMITTED_VALUE))
                .build();
        assertEquals(expected.getId(), taskResponse.getId());
        assertEquals(expected.getContextId(), taskResponse.getContextId());
        assertEquals(expected.getStatus().getState(), taskResponse.getStatus().getState());
        assertEquals(expected.getHistoryList(), taskResponse.getHistoryList());
    }

    @Test
    public void testOnMessageStreamNewMessageExistingTaskSuccessMocks() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);

        io.a2a.spec.Task task = new io.a2a.spec.Task.Builder(AbstractA2ARequestHandlerTest.MINIMAL_TASK)
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
                        .status(new io.a2a.spec.TaskStatus(io.a2a.spec.TaskState.WORKING))
                        .build());

        StreamRecorder<StreamResponse> streamRecorder;
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            streamRecorder = sendStreamingMessageRequest(handler);
        }
        Assertions.assertNull(streamRecorder.getError());
        List<StreamResponse> result = streamRecorder.getValues();
        Assertions.assertEquals(2, result.size());
        StreamResponse first = result.get(0);
        Assertions.assertTrue(first.hasArtifactUpdate());
        io.a2a.grpc.TaskArtifactUpdateEvent taskArtifactUpdateEvent = first.getArtifactUpdate();
        assertEquals(task.getId(), taskArtifactUpdateEvent.getTaskId());
        assertEquals(task.getContextId(), taskArtifactUpdateEvent.getContextId());
        assertEquals("11", taskArtifactUpdateEvent.getArtifact().getArtifactId());
        assertEquals("text", taskArtifactUpdateEvent.getArtifact().getParts(0).getText());
        StreamResponse second = result.get(1);
        Assertions.assertTrue(second.hasStatusUpdate());
        io.a2a.grpc.TaskStatusUpdateEvent taskStatusUpdateEvent = second.getStatusUpdate();
        assertEquals(task.getId(), taskStatusUpdateEvent.getTaskId());
        assertEquals(task.getContextId(), taskStatusUpdateEvent.getContextId());
        assertEquals(TaskState.TASK_STATE_WORKING, taskStatusUpdateEvent.getStatus().getState());
    }

    @Test
    public void testOnMessageStreamNewMessageSendPushNotificationSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        List<Event> events = List.of(
                AbstractA2ARequestHandlerTest.MINIMAL_TASK,
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                        .contextId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                        .contextId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId())
                        .status(new io.a2a.spec.TaskStatus(io.a2a.spec.TaskState.COMPLETED))
                        .build());


        agentExecutorExecute = (context, eventQueue) -> {
            // Hardcode the events to send here
            for (Event event : events) {
                eventQueue.enqueueEvent(event);
            }
        };

        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        StreamRecorder<TaskPushNotificationConfig> pushStreamRecorder = createTaskPushNotificationConfigRequest(handler, NAME);
        Assertions.assertNull(pushStreamRecorder.getError());

        List<StreamResponse> results = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(6);
        httpClient.latch = latch;
        StreamObserver<StreamResponse> streamObserver = new StreamObserver<>() {
            @Override
            public void onNext(StreamResponse streamResponse) {
                results.add(streamResponse);
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errors.add(throwable);
            }

            @Override
            public void onCompleted() {
            }
        };
        sendStreamingMessageRequest(handler, streamObserver);
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
        Assertions.assertTrue(errors.isEmpty());
        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals(3, httpClient.tasks.size());

        io.a2a.spec.Task curr = httpClient.tasks.get(0);
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId(), curr.getId());
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId(), curr.getContextId());
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        Assertions.assertEquals(0, curr.getArtifacts() == null ? 0 : curr.getArtifacts().size());

        curr = httpClient.tasks.get(1);
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId(), curr.getId());
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId(), curr.getContextId());
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getStatus().state(), curr.getStatus().state());
        Assertions.assertEquals(1, curr.getArtifacts().size());
        Assertions.assertEquals(1, curr.getArtifacts().get(0).parts().size());
        Assertions.assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());

        curr = httpClient.tasks.get(2);
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId(), curr.getId());
        Assertions.assertEquals(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId(), curr.getContextId());
        Assertions.assertEquals(io.a2a.spec.TaskState.COMPLETED, curr.getStatus().state());
        Assertions.assertEquals(1, curr.getArtifacts().size());
        Assertions.assertEquals(1, curr.getArtifacts().get(0).parts().size());
        Assertions.assertEquals("text", ((TextPart)curr.getArtifacts().get(0).parts().get(0)).getText());
    }

    @Test
    public void testOnResubscribeNoExistingTaskError() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        TaskSubscriptionRequest request = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<StreamResponse> streamRecorder = StreamRecorder.create();
        handler.taskSubscription(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        assertGrpcError(streamRecorder, Status.Code.NOT_FOUND);
    }

    @Test
    public void testOnResubscribeExistingTaskSuccess() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        queueManager.createOrTap(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId());

        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getMessage());
        };

        StreamRecorder<StreamResponse> streamRecorder = StreamRecorder.create();
        TaskSubscriptionRequest request = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        handler.taskSubscription(request, streamRecorder);

        // We need to send some events in order for those to end up in the queue
        SendMessageRequest sendMessageRequest = SendMessageRequest.newBuilder()
                .setRequest(GRPC_MESSAGE)
                .build();
        StreamRecorder<StreamResponse> messageRecorder = StreamRecorder.create();
        handler.sendStreamingMessage(sendMessageRequest, messageRecorder);
        messageRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        Assertions.assertNull(messageRecorder.getError());

        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        List<StreamResponse> result = streamRecorder.getValues();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        StreamResponse response = result.get(0);
        Assertions.assertTrue(response.hasMsg());
        assertEquals(GRPC_MESSAGE, response.getMsg());
        Assertions.assertNull(streamRecorder.getError());
    }

    @Test
    public void testOnResubscribeExistingTaskSuccessMocks() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        queueManager.createOrTap(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId());

        List<Event> events = List.of(
                new TaskArtifactUpdateEvent.Builder()
                        .taskId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                        .contextId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId())
                        .artifact(new Artifact.Builder()
                                .artifactId("11")
                                .parts(new TextPart("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent.Builder()
                        .taskId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                        .contextId(AbstractA2ARequestHandlerTest.MINIMAL_TASK.getContextId())
                        .status(new io.a2a.spec.TaskStatus(io.a2a.spec.TaskState.WORKING))
                        .build());

        StreamRecorder<StreamResponse> streamRecorder = StreamRecorder.create();
        TaskSubscriptionRequest request = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        try (MockedConstruction<EventConsumer> mocked = Mockito.mockConstruction(
                EventConsumer.class,
                (mock, context) -> {
                    Mockito.doReturn(ZeroPublisher.fromIterable(events)).when(mock).consumeAll();})){
            handler.taskSubscription(request, streamRecorder);
            streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        }
        List<StreamResponse> result = streamRecorder.getValues();
        Assertions.assertEquals(events.size(), result.size());
        StreamResponse first = result.get(0);
        Assertions.assertTrue(first.hasArtifactUpdate());
        io.a2a.grpc.TaskArtifactUpdateEvent event = first.getArtifactUpdate();
        assertEquals("11", event.getArtifact().getArtifactId());
        assertEquals("text", (event.getArtifact().getParts(0)).getText());
        StreamResponse second = result.get(1);
        Assertions.assertTrue(second.hasStatusUpdate());
        assertEquals(TaskState.TASK_STATE_WORKING, second.getStatusUpdate().getStatus().getState());
    }

    @Test
    public void testStreamingNotSupportedError() throws Exception {
        AgentCard card = AbstractA2ARequestHandlerTest.createAgentCard(false, true, true);
        GrpcHandler handler = new GrpcHandler(card, requestHandler);
        StreamRecorder<StreamResponse> streamRecorder = sendStreamingMessageRequest(handler);
        assertGrpcError(streamRecorder, Status.Code.INVALID_ARGUMENT);
    }

    @Test
    public void testStreamingNotSupportedErrorOnResubscribeToTask() throws Exception {
        // This test does not exist in the Python implementation
        AgentCard card = AbstractA2ARequestHandlerTest.createAgentCard(false, true, true);
        GrpcHandler handler = new GrpcHandler(card, requestHandler);
        TaskSubscriptionRequest request = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<StreamResponse> streamRecorder = StreamRecorder.create();
        handler.taskSubscription(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        assertGrpcError(streamRecorder, Status.Code.INVALID_ARGUMENT);
    }

    @Test
    public void testOnMessageStreamInternalError() throws Exception {
        DefaultRequestHandler mocked = Mockito.mock(DefaultRequestHandler.class);
        Mockito.doThrow(new InternalError("Internal Error")).when(mocked).onMessageSendStream(Mockito.any(MessageSendParams.class), Mockito.any(ServerCallContext.class));
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, mocked);
        StreamRecorder<StreamResponse> streamRecorder = sendStreamingMessageRequest(handler);
        assertGrpcError(streamRecorder, Status.Code.INTERNAL);
    }

    @Test
    public void testListPushNotificationConfig() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        StreamRecorder<TaskPushNotificationConfig> pushRecorder = createTaskPushNotificationConfigRequest(handler, NAME);
        Assertions.assertNull(pushRecorder.getError());

        ListTaskPushNotificationConfigRequest request = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<ListTaskPushNotificationConfigResponse> streamRecorder =  StreamRecorder.create();
        handler.listTaskPushNotificationConfig(request, streamRecorder);
        Assertions.assertNull(streamRecorder.getError());
        List<ListTaskPushNotificationConfigResponse> result = streamRecorder.getValues();
        Assertions.assertEquals(1, result.size());
        List<TaskPushNotificationConfig> configList = result.get(0).getConfigsList();
        Assertions.assertEquals(1, configList.size());
        Assertions.assertEquals(pushRecorder.getValues().get(0), configList.get(0));
    }

    @Test
    public void testListPushNotificationConfigNotSupported() throws Exception {
        AgentCard card = AbstractA2ARequestHandlerTest.createAgentCard(true, false, true);
        GrpcHandler handler = new GrpcHandler(card, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest request = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<ListTaskPushNotificationConfigResponse> streamRecorder =  StreamRecorder.create();
        handler.listTaskPushNotificationConfig(request, streamRecorder);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testListPushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest request = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<ListTaskPushNotificationConfigResponse> streamRecorder =  StreamRecorder.create();
        handler.listTaskPushNotificationConfig(request, streamRecorder);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testListPushNotificationConfigTaskNotFound() {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        ListTaskPushNotificationConfigRequest request = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId())
                .build();
        StreamRecorder<ListTaskPushNotificationConfigResponse> streamRecorder =  StreamRecorder.create();
        handler.listTaskPushNotificationConfig(request, streamRecorder);
        assertGrpcError(streamRecorder, Status.Code.NOT_FOUND);
    }

    @Test
    public void testDeletePushNotificationConfig() throws Exception {
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        StreamRecorder<TaskPushNotificationConfig> pushRecorder = createTaskPushNotificationConfigRequest(handler, NAME);
        Assertions.assertNull(pushRecorder.getError());

        DeleteTaskPushNotificationConfigRequest request = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setName(NAME)
                .build();
        StreamRecorder<Empty> streamRecorder = StreamRecorder.create();
        handler.deleteTaskPushNotificationConfig(request, streamRecorder);
        Assertions.assertNull(streamRecorder.getError());
        Assertions.assertEquals(1, streamRecorder.getValues().size());
        assertEquals(Empty.getDefaultInstance(), streamRecorder.getValues().get(0));
    }

    @Test
    public void testDeletePushNotificationConfigNotSupported() throws Exception {
        AgentCard card = AbstractA2ARequestHandlerTest.createAgentCard(true, false, true);
        GrpcHandler handler = new GrpcHandler(card, requestHandler);
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        agentExecutorExecute = (context, eventQueue) -> {
            eventQueue.enqueueEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        DeleteTaskPushNotificationConfigRequest request = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setName(NAME)
                .build();
        StreamRecorder<Empty> streamRecorder = StreamRecorder.create();
        handler.deleteTaskPushNotificationConfig(request, streamRecorder);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testDeletePushNotificationConfigNoPushConfigStore() {
        DefaultRequestHandler requestHandler =
                new DefaultRequestHandler(executor, taskStore, queueManager, null, null, internalExecutor);
        GrpcHandler handler = new GrpcHandler(AbstractA2ARequestHandlerTest.CARD, requestHandler);
        String NAME = "tasks/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + AbstractA2ARequestHandlerTest.MINIMAL_TASK.getId();
        DeleteTaskPushNotificationConfigRequest request = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setName(NAME)
                .build();
        StreamRecorder<Empty> streamRecorder = StreamRecorder.create();
        handler.deleteTaskPushNotificationConfig(request, streamRecorder);
        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    private StreamRecorder<SendMessageResponse> sendMessageRequest(GrpcHandler handler) throws Exception {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setRequest(GRPC_MESSAGE)
                .build();
        StreamRecorder<SendMessageResponse> streamRecorder = StreamRecorder.create();
        handler.sendMessage(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }

    private StreamRecorder<TaskPushNotificationConfig> createTaskPushNotificationConfigRequest(GrpcHandler handler, String name) throws Exception {
        taskStore.save(AbstractA2ARequestHandlerTest.MINIMAL_TASK);
        PushNotificationConfig config = PushNotificationConfig.newBuilder()
                .setUrl("http://example.com")
                .build();
        TaskPushNotificationConfig taskPushNotificationConfig = TaskPushNotificationConfig.newBuilder()
                .setName(name)
                .setPushNotificationConfig(config)
                .build();
        CreateTaskPushNotificationConfigRequest setRequest = CreateTaskPushNotificationConfigRequest.newBuilder()
                .setConfig(taskPushNotificationConfig)
                .build();

        StreamRecorder<TaskPushNotificationConfig> streamRecorder = StreamRecorder.create();
        handler.createTaskPushNotificationConfig(setRequest, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }

    private StreamRecorder<TaskPushNotificationConfig> getTaskPushNotificationConfigRequest(GrpcHandler handler, String name) throws Exception {
        GetTaskPushNotificationConfigRequest request = GetTaskPushNotificationConfigRequest.newBuilder()
                .setName(name)
                .build();
        StreamRecorder<TaskPushNotificationConfig> streamRecorder = StreamRecorder.create();
        handler.getTaskPushNotificationConfig(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }

    private StreamRecorder<StreamResponse> sendStreamingMessageRequest(GrpcHandler handler) throws Exception {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setRequest(GRPC_MESSAGE)
                .build();
        StreamRecorder<StreamResponse> streamRecorder = StreamRecorder.create();
        handler.sendStreamingMessage(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }

    private void sendStreamingMessageRequest(GrpcHandler handler, StreamObserver<StreamResponse> streamObserver) throws Exception {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setRequest(GRPC_MESSAGE)
                .build();
        handler.sendStreamingMessage(request, streamObserver);
    }

    private <V> void assertGrpcError(StreamRecorder<V> streamRecorder, Status.Code expectedStatusCode) {
        Assertions.assertNotNull(streamRecorder.getError());
        Assertions.assertInstanceOf(StatusRuntimeException.class, streamRecorder.getError());
        Assertions.assertEquals(expectedStatusCode, ((StatusRuntimeException) streamRecorder.getError()).getStatus().getCode());
        Assertions.assertTrue(streamRecorder.getValues().isEmpty());
    }
}
