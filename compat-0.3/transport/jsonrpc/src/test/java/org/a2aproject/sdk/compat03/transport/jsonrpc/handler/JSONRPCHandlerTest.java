package org.a2aproject.sdk.compat03.transport.jsonrpc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Map;

import org.a2aproject.sdk.compat03.conversion.AbstractA2ARequestHandlerTest;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.junit.jupiter.api.Test;

// V0.3 spec imports (client perspective)
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest;
import org.a2aproject.sdk.compat03.spec.CancelTaskResponse;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest;
import org.a2aproject.sdk.compat03.spec.GetTaskResponse;
import org.a2aproject.sdk.compat03.spec.Message;
import org.a2aproject.sdk.compat03.spec.MessageSendParams;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest;
import org.a2aproject.sdk.compat03.spec.SendMessageResponse;
import org.a2aproject.sdk.compat03.spec.Task;
import org.a2aproject.sdk.compat03.spec.TaskIdParams;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams;
import org.a2aproject.sdk.compat03.spec.TaskState;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError;

/**
 * Test suite for v0.3 JSONRPCHandler with v1.0 backend.
 * <p>
 * Tests verify that v0.3 clients can successfully communicate with the v1.0 backend
 * via the {@link org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler} conversion layer.
 * </p>
 * <p>
 * <b>Phase 2 Focus:</b> Core non-streaming tests (GetTask, SendMessage, CancelTask).
 * Streaming tests and push notification tests are deferred to later phases.
 * </p>
 */
public class JSONRPCHandlerTest extends AbstractA2ARequestHandlerTest {

    private final ServerCallContext callContext = new ServerCallContext(
            UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

    // ========================================
    // GetTask Tests
    // ========================================

    @Test
    public void testOnGetTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0 for taskStore
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
        GetTaskResponse response = handler.onGetTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertNull(response.getError());

        // Response should contain v0.3 task (converted back from v1.0)
        Task result = response.getResult();
        assertEquals(MINIMAL_TASK.getId(), result.getId());
        assertEquals(MINIMAL_TASK.getContextId(), result.getContextId());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
        GetTaskResponse response = handler.onGetTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertInstanceOf(TaskNotFoundError.class, response.getError());
        assertNull(response.getResult());
    }

    // ========================================
    // CancelTask Tests
    // ========================================

    @Test
    public void testOnCancelTaskSuccess() throws Exception {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to cancel the task
        // In v1.0, we use AgentEmitter.cancel() instead of TaskUpdater
        agentExecutorCancel = (context, emitter) -> {
            emitter.cancel();
        };

        CancelTaskRequest request = new CancelTaskRequest("111", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);

        assertNull(response.getError());
        assertEquals(request.getId(), response.getId());

        // Verify task was canceled
        Task task = response.getResult();
        assertEquals(MINIMAL_TASK.getId(), task.getId());
        assertEquals(MINIMAL_TASK.getContextId(), task.getContextId());
        assertEquals(TaskState.CANCELED, task.getStatus().state());
    }

    @Test
    public void testOnCancelTaskNotSupported() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to throw UnsupportedOperationError
        agentExecutorCancel = (context, emitter) -> {
            throw new org.a2aproject.sdk.spec.UnsupportedOperationError();
        };

        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
    }

    @Test
    public void testOnCancelTaskNotFound() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));
        CancelTaskResponse response = handler.onCancelTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(TaskNotFoundError.class, response.getError());
    }

    // ========================================
    // SendMessage Tests (Non-Streaming)
    // ========================================

    @Test
    public void testOnMessageSendSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Configure agent to echo the message back
        agentExecutorExecute = (context, emitter) -> {
            // Note: context.getMessage() contains v1.0 Message (already converted by Convert03To10RequestHandler)
            // Emit the v1.0 message, it will be converted back to v0.3 in the response
            emitter.emitEvent(context.getMessage());
        };

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);

        assertNull(response.getError());
        // Response should contain the message (converted back from v1.0)
        org.a2aproject.sdk.compat03.spec.EventKind result = response.getResult();
        if (result instanceof Message) {
            assertEquals(message.getMessageId(), ((Message) result).getMessageId());
        }
    }

    @Test
    public void testOnMessageSendWithExistingTaskSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to emit message
        agentExecutorExecute = (context, emitter) -> {
            // Emit v1.0 message from context
            emitter.emitEvent(context.getMessage());
        };

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);

        assertNull(response.getError());
        org.a2aproject.sdk.compat03.spec.EventKind result = response.getResult();
        if (result instanceof Message) {
            assertEquals(message.getMessageId(), ((Message) result).getMessageId());
        }
    }

    @Test
    public void testOnMessageSendError() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Configure agent to throw error
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(new org.a2aproject.sdk.spec.UnsupportedOperationError());
        };

        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = handler.onMessageSend(request, callContext);

        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertNull(response.getResult());
    }

    // ========================================
    // Deferred Tests (Phase 4+)
    // ========================================

    // TODO Phase 4: Add streaming tests (testOnMessageSendStreamSuccess, etc.)
    // TODO Phase 4: Add multi-event streaming tests
    // TODO Phase 5: Add push notification tests
    // TODO Phase 6: Add authenticated extended card tests
}
