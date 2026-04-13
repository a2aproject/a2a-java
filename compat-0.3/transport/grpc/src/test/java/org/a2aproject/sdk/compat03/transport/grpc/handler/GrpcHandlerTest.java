package org.a2aproject.sdk.compat03.transport.grpc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.compat03.conversion.AbstractA2ARequestHandlerTest;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
import org.a2aproject.sdk.compat03.grpc.Task;
import org.a2aproject.sdk.compat03.grpc.TaskState;

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

    private <V> void assertGrpcError(StreamRecorder<V> streamRecorder, Status.Code expectedStatusCode) {
        Assertions.assertNotNull(streamRecorder.getError());
        Assertions.assertInstanceOf(StatusRuntimeException.class, streamRecorder.getError());
        Assertions.assertEquals(expectedStatusCode, ((StatusRuntimeException) streamRecorder.getError()).getStatus().getCode());
        Assertions.assertTrue(streamRecorder.getValues().isEmpty());
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

    // ========================================
    // Deferred Tests (Phase 4+)
    // ========================================

    // TODO Phase 4: Add streaming tests (testOnMessageStreamNewMessageSuccess, etc.)
    // TODO Phase 4: Add multi-event streaming tests
    // TODO Phase 5: Add push notification tests
}
