package org.a2aproject.sdk.compat03.transport.grpc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.compat03.conversion.AbstractA2ARequestHandlerTest;
import org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

// gRPC test imports
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.testing.StreamRecorder;

// v0.3 gRPC proto imports
import org.a2aproject.sdk.compat03.grpc.CancelTaskRequest;
import org.a2aproject.sdk.compat03.grpc.GetTaskRequest;
import org.a2aproject.sdk.compat03.grpc.Message;
import org.a2aproject.sdk.compat03.grpc.Part;
import org.a2aproject.sdk.compat03.grpc.Role;
import org.a2aproject.sdk.compat03.grpc.SendMessageRequest;
import org.a2aproject.sdk.compat03.grpc.SendMessageResponse;
import org.a2aproject.sdk.compat03.grpc.StreamResponse;
import org.a2aproject.sdk.compat03.grpc.Task;
import org.a2aproject.sdk.compat03.grpc.TaskState;
import org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest;

/**
 * Test suite for v0.3 GrpcHandler with v1.0 backend.
 * <p>
 * Tests verify that v0.3 gRPC clients can successfully communicate with the v1.0 backend
 * via the {@link org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler} conversion layer.
 * </p>
 * <p>
 * <b>Phase 3 Focus:</b> Core non-streaming tests (GetTask, SendMessage, CancelTask).
 * Streaming tests are deferred to Phase 4.
 * </p>
 */
public class GrpcHandlerTest extends AbstractA2ARequestHandlerTest {

    // gRPC Message fixture (protobuf format)
    private static final Message GRPC_MESSAGE = Message.newBuilder()
            .setTaskId(MINIMAL_TASK.getId())
            .setContextId(MINIMAL_TASK.getContextId())
            .setMessageId(MESSAGE.getMessageId())
            .setRole(Role.ROLE_AGENT)
            .addContent(Part.newBuilder().setText(((org.a2aproject.sdk.compat03.spec.TextPart) MESSAGE.getParts().get(0)).getText()).build())
            .build();

    private final ServerCallContext callContext = new ServerCallContext(
            UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

    // ========================================
    // GetTask Tests
    // ========================================

    @Test
    public void testOnGetTaskSuccess() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        GetTaskRequest request = GetTaskRequest.newBuilder()
                .setName("tasks/" + MINIMAL_TASK.getId())
                .build();

        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.getTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        Assertions.assertNull(streamRecorder.getError());
        List<Task> result = streamRecorder.getValues();
        Assertions.assertEquals(1, result.size());
        Task task = result.get(0);
        assertEquals(MINIMAL_TASK.getId(), task.getId());
        assertEquals(MINIMAL_TASK.getContextId(), task.getContextId());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        GetTaskRequest request = GetTaskRequest.newBuilder()
                .setName("tasks/" + MINIMAL_TASK.getId())
                .build();

        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.getTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertGrpcError(streamRecorder, Status.Code.NOT_FOUND);
    }

    // ========================================
    // CancelTask Tests
    // ========================================

    @Test
    public void testOnCancelTaskSuccess() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to cancel the task
        agentExecutorCancel = (context, emitter) -> {
            emitter.cancel();
        };

        CancelTaskRequest request = CancelTaskRequest.newBuilder()
                .setName("tasks/" + MINIMAL_TASK.getId())
                .build();

        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.cancelTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        Assertions.assertNull(streamRecorder.getError());
        List<Task> result = streamRecorder.getValues();
        Assertions.assertEquals(1, result.size());
        Task task = result.get(0);
        assertEquals(MINIMAL_TASK.getId(), task.getId());
        assertEquals(MINIMAL_TASK.getContextId(), task.getContextId());
        assertEquals(TaskState.TASK_STATE_CANCELLED, task.getStatus().getState());
    }

    @Test
    public void testOnCancelTaskNotSupported() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to throw UnsupportedOperationError
        agentExecutorCancel = (context, emitter) -> {
            throw new org.a2aproject.sdk.spec.UnsupportedOperationError();
        };

        CancelTaskRequest request = CancelTaskRequest.newBuilder()
                .setName("tasks/" + MINIMAL_TASK.getId())
                .build();

        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.cancelTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    @Test
    public void testOnCancelTaskNotFound() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        CancelTaskRequest request = CancelTaskRequest.newBuilder()
                .setName("tasks/" + MINIMAL_TASK.getId())
                .build();

        StreamRecorder<Task> streamRecorder = StreamRecorder.create();
        handler.cancelTask(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertGrpcError(streamRecorder, Status.Code.NOT_FOUND);
    }

    // ========================================
    // SendMessage Tests (Non-Streaming)
    // ========================================

    @Test
    public void testOnMessageNewMessageSuccess() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Configure agent to echo the message back
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(context.getMessage());
        };

        StreamRecorder<SendMessageResponse> streamRecorder = sendMessageRequest(handler);

        Assertions.assertNull(streamRecorder.getError());
        List<SendMessageResponse> result = streamRecorder.getValues();
        Assertions.assertEquals(1, result.size());
        SendMessageResponse response = result.get(0);
        Assertions.assertTrue(response.hasMsg());
        Message message = response.getMsg();
        assertEquals(GRPC_MESSAGE.getMessageId(), message.getMessageId());
    }

    @Test
    public void testOnMessageNewMessageWithExistingTaskSuccess() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Save existing task
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to emit message
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(context.getMessage());
        };

        StreamRecorder<SendMessageResponse> streamRecorder = sendMessageRequest(handler);

        Assertions.assertNull(streamRecorder.getError());
        List<SendMessageResponse> result = streamRecorder.getValues();
        Assertions.assertEquals(1, result.size());
        SendMessageResponse response = result.get(0);
        Assertions.assertTrue(response.hasMsg());
        Message message = response.getMsg();
        assertEquals(GRPC_MESSAGE.getMessageId(), message.getMessageId());
    }

    @Test
    public void testOnMessageError() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Configure agent to throw error
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(new org.a2aproject.sdk.spec.UnsupportedOperationError());
        };

        StreamRecorder<SendMessageResponse> streamRecorder = sendMessageRequest(handler);

        assertGrpcError(streamRecorder, Status.Code.UNIMPLEMENTED);
    }

    // ========================================
    // Streaming Tests
    // ========================================

    @Test
    public void testOnMessageStreamNewMessageSuccess() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Configure agent to emit the message back (v1.0 context contains v1.0 Message)
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(context.getMessage());
        };

        StreamRecorder<StreamResponse> streamRecorder = sendStreamingMessageRequest(handler);

        assertNull(streamRecorder.getError(), "No error should occur");
        List<StreamResponse> result = streamRecorder.getValues();
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should receive exactly 1 event");

        StreamResponse response = result.get(0);
        assertTrue(response.hasMsg(), "Response should contain a message");
        Message message = response.getMsg();
        assertEquals(GRPC_MESSAGE.getMessageId(), message.getMessageId());
    }

    @Test
    public void testStreamingNotSupportedError() throws Exception {
        // Create agent card with streaming disabled
        org.a2aproject.sdk.compat03.spec.AgentCard nonStreamingCard =
                new org.a2aproject.sdk.compat03.spec.AgentCard.Builder(CARD)
                        .capabilities(new org.a2aproject.sdk.compat03.spec.AgentCapabilities(false, true, false, null))
                        .build();

        TestGrpcHandler handler = new TestGrpcHandler(nonStreamingCard, convert03To10Handler, internalExecutor);

        StreamRecorder<StreamResponse> streamRecorder = sendStreamingMessageRequest(handler);

        // Should receive INVALID_ARGUMENT status
        assertGrpcError(streamRecorder, Status.Code.INVALID_ARGUMENT);
    }

    @Test
    public void testStreamingNotSupportedErrorOnResubscribeToTask() throws Exception {
        // Create agent card with streaming disabled
        org.a2aproject.sdk.compat03.spec.AgentCard nonStreamingCard =
                new org.a2aproject.sdk.compat03.spec.AgentCard.Builder(CARD)
                        .capabilities(new org.a2aproject.sdk.compat03.spec.AgentCapabilities(false, true, false, null))
                        .build();

        TestGrpcHandler handler = new TestGrpcHandler(nonStreamingCard, convert03To10Handler, internalExecutor);

        TaskSubscriptionRequest request = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/" + MINIMAL_TASK.getId())
                .build();

        StreamRecorder<StreamResponse> streamRecorder = StreamRecorder.create();
        handler.taskSubscription(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        // Should receive INVALID_ARGUMENT status
        assertGrpcError(streamRecorder, Status.Code.INVALID_ARGUMENT);
    }

    @Test
    public void testOnMessageStreamInternalError() throws Exception {
        // Mock the Convert03To10RequestHandler to throw InternalError
        Convert03To10RequestHandler mockedHandler = Mockito.mock(Convert03To10RequestHandler.class);
        Mockito.doThrow(new org.a2aproject.sdk.spec.InternalError("Internal Error"))
                .when(mockedHandler)
                .onMessageSendStream(
                        Mockito.any(org.a2aproject.sdk.compat03.spec.MessageSendParams.class),
                        Mockito.any(ServerCallContext.class));

        TestGrpcHandler handler = new TestGrpcHandler(CARD, mockedHandler, internalExecutor);

        StreamRecorder<StreamResponse> streamRecorder = sendStreamingMessageRequest(handler);

        // Should receive INTERNAL status
        assertGrpcError(streamRecorder, Status.Code.INTERNAL);
    }

    // ========================================
    // Push Notification Tests
    // ========================================

    @Test
    public void testSetPushNotificationConfigSuccess() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        String NAME = "tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/config456";
        StreamRecorder<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> streamRecorder =
                createTaskPushNotificationConfigRequest(handler, NAME);

        assertNull(streamRecorder.getError());
        List<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> result = streamRecorder.getValues();
        assertNotNull(result);
        assertEquals(1, result.size());
        org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig response = result.get(0);
        assertEquals(NAME, response.getName());
        org.a2aproject.sdk.compat03.grpc.PushNotificationConfig responseConfig = response.getPushNotificationConfig();
        assertEquals("config456", responseConfig.getId());
        assertEquals("http://example.com", responseConfig.getUrl());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        String NAME = "tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/config456";

        // First set the task push notification config
        StreamRecorder<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> streamRecorder =
                createTaskPushNotificationConfigRequest(handler, NAME);
        assertNull(streamRecorder.getError());

        // Then get the task push notification config
        streamRecorder = getTaskPushNotificationConfigRequest(handler, NAME);
        assertNull(streamRecorder.getError());
        List<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> result = streamRecorder.getValues();
        assertNotNull(result);
        assertEquals(1, result.size());
        org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig response = result.get(0);
        assertEquals(NAME, response.getName());
        org.a2aproject.sdk.compat03.grpc.PushNotificationConfig responseConfig = response.getPushNotificationConfig();
        assertEquals("config456", responseConfig.getId());
        assertEquals("http://example.com", responseConfig.getUrl());
    }

    @Test
    public void testPushNotificationsNotSupportedError() throws Exception {
        org.a2aproject.sdk.compat03.spec.AgentCard card = createAgentCard(true, false, false);
        TestGrpcHandler handler = new TestGrpcHandler(card, convert03To10Handler, internalExecutor);

        String NAME = "tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + MINIMAL_TASK.getId();
        StreamRecorder<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> streamRecorder =
                createTaskPushNotificationConfigRequest(handler, NAME);

        assertNotNull(streamRecorder.getError());
        assertInstanceOf(StatusRuntimeException.class, streamRecorder.getError());
        StatusRuntimeException error = (StatusRuntimeException) streamRecorder.getError();
        assertEquals(Status.Code.UNIMPLEMENTED, error.getStatus().getCode());
    }

    @Test
    public void testDeletePushNotificationConfig() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        String NAME = "tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + MINIMAL_TASK.getId();
        org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequest request =
                org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder()
                        .setName(NAME)
                        .build();

        StreamRecorder<com.google.protobuf.Empty> streamRecorder = StreamRecorder.create();
        handler.deleteTaskPushNotificationConfig(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertNull(streamRecorder.getError());
        List<com.google.protobuf.Empty> result = streamRecorder.getValues();
        assertEquals(1, result.size());
    }

    @Test
    public void testListPushNotificationConfig() throws Exception {
        TestGrpcHandler handler = new TestGrpcHandler(CARD, convert03To10Handler, internalExecutor);

        // Save task to v1.0 backend
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        String PARENT = "tasks/" + MINIMAL_TASK.getId();
        org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest request =
                org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest.newBuilder()
                        .setParent(PARENT)
                        .build();

        StreamRecorder<org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse> streamRecorder = StreamRecorder.create();
        handler.listTaskPushNotificationConfig(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);

        assertNull(streamRecorder.getError());
        List<org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse> result = streamRecorder.getValues();
        assertEquals(1, result.size());
    }

    // ========================================
    // Helper Methods
    // ========================================

    private StreamRecorder<SendMessageResponse> sendMessageRequest(TestGrpcHandler handler) throws Exception {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setRequest(GRPC_MESSAGE)
                .build();
        StreamRecorder<SendMessageResponse> streamRecorder = StreamRecorder.create();
        handler.sendMessage(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }

    private StreamRecorder<StreamResponse> sendStreamingMessageRequest(TestGrpcHandler handler) throws Exception {
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setRequest(GRPC_MESSAGE)
                .build();
        StreamRecorder<StreamResponse> streamRecorder = StreamRecorder.create();
        handler.sendStreamingMessage(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }

    private <V> void assertGrpcError(StreamRecorder<V> streamRecorder, Status.Code expectedStatusCode) {
        Assertions.assertNotNull(streamRecorder.getError());
        Assertions.assertInstanceOf(StatusRuntimeException.class, streamRecorder.getError());
        Assertions.assertEquals(expectedStatusCode, ((StatusRuntimeException) streamRecorder.getError()).getStatus().getCode());
        Assertions.assertTrue(streamRecorder.getValues().isEmpty());
    }


    private StreamRecorder<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> createTaskPushNotificationConfigRequest(
            TestGrpcHandler handler, String name) throws Exception {
        // Save task to v1.0 backend
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        org.a2aproject.sdk.compat03.grpc.PushNotificationConfig config =
                org.a2aproject.sdk.compat03.grpc.PushNotificationConfig.newBuilder()
                        .setUrl("http://example.com")
                        .build();

        org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig taskPushNotificationConfig =
                org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder()
                        .setName(name)
                        .setPushNotificationConfig(config)
                        .build();

        org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest setRequest =
                org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest.newBuilder()
                        .setConfig(taskPushNotificationConfig)
                        .build();

        StreamRecorder<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> streamRecorder = StreamRecorder.create();
        handler.createTaskPushNotificationConfig(setRequest, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }

    private StreamRecorder<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> getTaskPushNotificationConfigRequest(
            TestGrpcHandler handler, String name) throws Exception {
        org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest request =
                org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest.newBuilder()
                        .setName(name)
                        .build();

        StreamRecorder<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> streamRecorder = StreamRecorder.create();
        handler.getTaskPushNotificationConfig(request, streamRecorder);
        streamRecorder.awaitCompletion(5, TimeUnit.SECONDS);
        return streamRecorder;
    }
    // ========================================
    // Test Handler Implementation
    // ========================================

    private static class TestGrpcHandler extends GrpcHandler {
        private final org.a2aproject.sdk.compat03.spec.AgentCard card;
        private final org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler handler;
        private final java.util.concurrent.Executor executor;

        TestGrpcHandler(org.a2aproject.sdk.compat03.spec.AgentCard card,
                        org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler handler,
                        java.util.concurrent.Executor executor) {
            this.card = card;
            this.handler = handler;
            this.executor = executor;
            setRequestHandler(handler);
        }

        @Override
        protected org.a2aproject.sdk.compat03.spec.AgentCard getAgentCard() {
            return card;
        }

        @Override
        protected org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler getRequestHandler() {
            return handler;
        }

        @Override
        protected CallContextFactory getCallContextFactory() {
            return null;
        }

        @Override
        protected java.util.concurrent.Executor getExecutor() {
            return executor;
        }
    }

}
