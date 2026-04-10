package org.a2aproject.sdk.compat03.transport.jsonrpc.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.compat03.conversion.AbstractA2ARequestHandlerTest_v0_3;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskArtifactUpdateEventMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskStatusUpdateEventMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Event_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

// V0.3 spec imports (client perspective)
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;

/**
 * Test suite for v0.3 JSONRPCHandler with v1.0 backend.
 * <p>
 * Tests verify that v0.3 clients can successfully communicate with the v1.0 backend
 * via the {@link Convert_v0_3_To10RequestHandler} conversion layer.
 * </p>
 * <p>
 * <b>Phase 2 Focus:</b> Core non-streaming tests (GetTask, SendMessage, CancelTask).
 * Streaming tests and push notification tests are deferred to later phases.
 * </p>
 */
public class JSONRPCHandler_v0_3_Test extends AbstractA2ARequestHandlerTest_v0_3 {

    private final ServerCallContext callContext = new ServerCallContext(
            UnauthenticatedUser.INSTANCE, Map.of("foo", "bar"), new HashSet<>());

    // ========================================
    // GetTask Tests
    // ========================================

    @Test
    public void testOnGetTaskSuccess() throws Exception {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0 for taskStore
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        GetTaskRequest_v0_3 request = new GetTaskRequest_v0_3("1", new TaskQueryParams_v0_3(MINIMAL_TASK.getId()));
        GetTaskResponse_v0_3 response = handler.onGetTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertNull(response.getError());

        // Response should contain v0.3 task (converted back from v1.0)
        Task_v0_3 result = response.getResult();
        assertEquals(MINIMAL_TASK.getId(), result.getId());
        assertEquals(MINIMAL_TASK.getContextId(), result.getContextId());
    }

    @Test
    public void testOnGetTaskNotFound() throws Exception {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        GetTaskRequest_v0_3 request = new GetTaskRequest_v0_3("1", new TaskQueryParams_v0_3(MINIMAL_TASK.getId()));
        GetTaskResponse_v0_3 response = handler.onGetTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertInstanceOf(TaskNotFoundError_v0_3.class, response.getError());
        assertNull(response.getResult());
    }

    // ========================================
    // CancelTask Tests
    // ========================================

    @Test
    public void testOnCancelTaskSuccess() throws Exception {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to cancel the task
        // In v1.0, we use AgentEmitter.cancel() instead of TaskUpdater
        agentExecutorCancel = (context, emitter) -> {
            emitter.cancel();
        };

        CancelTaskRequest_v0_3 request = new CancelTaskRequest_v0_3("111", new TaskIdParams_v0_3(MINIMAL_TASK.getId()));
        CancelTaskResponse_v0_3 response = handler.onCancelTask(request, callContext);

        assertNull(response.getError());
        assertEquals(request.getId(), response.getId());

        // Verify task was canceled
        Task_v0_3 task = response.getResult();
        assertEquals(MINIMAL_TASK.getId(), task.getId());
        assertEquals(MINIMAL_TASK.getContextId(), task.getContextId());
        assertEquals(TaskState_v0_3.CANCELED, task.getStatus().state());
    }

    @Test
    public void testOnCancelTaskNotSupported() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save v0.3 task by converting to v1.0
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to throw UnsupportedOperationError
        agentExecutorCancel = (context, emitter) -> {
            throw new org.a2aproject.sdk.spec.UnsupportedOperationError();
        };

        CancelTaskRequest_v0_3 request = new CancelTaskRequest_v0_3("1", new TaskIdParams_v0_3(MINIMAL_TASK.getId()));
        CancelTaskResponse_v0_3 response = handler.onCancelTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(UnsupportedOperationError_v0_3.class, response.getError());
    }

    @Test
    public void testOnCancelTaskNotFound() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        CancelTaskRequest_v0_3 request = new CancelTaskRequest_v0_3("1", new TaskIdParams_v0_3(MINIMAL_TASK.getId()));
        CancelTaskResponse_v0_3 response = handler.onCancelTask(request, callContext);

        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        assertInstanceOf(TaskNotFoundError_v0_3.class, response.getError());
    }

    // ========================================
    // SendMessage Tests (Non-Streaming)
    // ========================================

    @Test
    public void testOnMessageSendSuccess() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to echo the message back
        agentExecutorExecute = (context, emitter) -> {
            // Note: context.getMessage() contains v1.0 Message (already converted by Convert03To10RequestHandler)
            // Emit the v1.0 message, it will be converted back to v0.3 in the response
            emitter.emitEvent(context.getMessage());
        };

        Message_v0_3 message = new Message_v0_3.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest_v0_3 request = new SendMessageRequest_v0_3("1", new MessageSendParams_v0_3(message, null, null));
        SendMessageResponse_v0_3 response = handler.onMessageSend(request, callContext);

        assertNull(response.getError());
        // Response should contain the message (converted back from v1.0)
        EventKind_v0_3 result = response.getResult();
        if (result instanceof Message_v0_3) {
            assertEquals(message.getMessageId(), ((Message_v0_3) result).getMessageId());
        }
    }

    @Test
    public void testOnMessageSendWithExistingTaskSuccess() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to emit message
        agentExecutorExecute = (context, emitter) -> {
            // Emit v1.0 message from context
            emitter.emitEvent(context.getMessage());
        };

        Message_v0_3 message = new Message_v0_3.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest_v0_3 request = new SendMessageRequest_v0_3("1", new MessageSendParams_v0_3(message, null, null));
        SendMessageResponse_v0_3 response = handler.onMessageSend(request, callContext);

        assertNull(response.getError());
        EventKind_v0_3 result = response.getResult();
        if (result instanceof Message_v0_3) {
            assertEquals(message.getMessageId(), ((Message_v0_3) result).getMessageId());
        }
    }

    @Test
    public void testOnMessageSendError() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to throw error
        agentExecutorExecute = (context, emitter) -> {
            emitter.emitEvent(new org.a2aproject.sdk.spec.UnsupportedOperationError());
        };

        Message_v0_3 message = new Message_v0_3.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendMessageRequest_v0_3 request = new SendMessageRequest_v0_3("1", new MessageSendParams_v0_3(message, null, null));
        SendMessageResponse_v0_3 response = handler.onMessageSend(request, callContext);

        assertInstanceOf(UnsupportedOperationError_v0_3.class, response.getError());
        assertNull(response.getResult());
    }

    // ========================================
    // Streaming Tests
    // ========================================

    @Test
    public void testOnMessageSendStreamSuccess() throws InterruptedException {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to emit the message back (v1.0 context contains v1.0 Message)
        agentExecutorExecute = (context, emitter) -> {
            // Emit v1.0 message - will be converted to v0.3 StreamingEventKind
            emitter.emitEvent(context.getMessage());
        };

        Message_v0_3 message = new Message_v0_3.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendStreamingMessageRequest_v0_3 request = new SendStreamingMessageRequest_v0_3(
                "1", new MessageSendParams_v0_3(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onMessageSendStream(request, callContext);

        List<StreamingEventKind_v0_3> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse_v0_3 item) {
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

        // Wait for event to be received
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected to receive 1 event within timeout");

        // Assert no error occurred during streaming
        assertNull(error.get(), "No error should occur during streaming");

        // Verify that the message was received
        assertEquals(1, results.size(), "Should have received exactly 1 event");

        // Verify the event is the message (converted back from v1.0)
        Message_v0_3 receivedMessage = assertInstanceOf(Message_v0_3.class, results.get(0), "Event should be a Message");
        assertEquals(message.getMessageId(), receivedMessage.getMessageId());
    }

    @Test
    public void testOnMessageSendStreamMultipleEventsSuccess() throws InterruptedException {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Create v0.3 events for reference (we'll emit v1.0 equivalents)
        Task_v0_3 v03TaskEvent = new Task_v0_3.Builder(MINIMAL_TASK)
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING))
                .build();

        TaskArtifactUpdateEvent_v0_3 v03ArtifactEvent = new TaskArtifactUpdateEvent_v0_3.Builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .artifact(new Artifact_v0_3.Builder()
                        .artifactId("artifact-1")
                        .parts(new TextPart_v0_3("Generated artifact content"))
                        .build())
                .build();

        TaskStatusUpdateEvent_v0_3 v03StatusEvent = new TaskStatusUpdateEvent_v0_3.Builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .isFinal(true) // Must be true for COMPLETED state in v1.0
                .build();

        // Configure the agent executor to emit multiple v1.0 events
        agentExecutorExecute = (context, emitter) -> {
            // Convert v0.3 events to v1.0 and emit them
            // The emitter will convert them back to v0.3 StreamingEventKind for the response
            emitter.emitEvent(TaskMapper_v0_3.INSTANCE.toV10(v03TaskEvent));
            emitter.emitEvent(TaskArtifactUpdateEventMapper_v0_3.INSTANCE.toV10(v03ArtifactEvent));
            emitter.emitEvent(TaskStatusUpdateEventMapper_v0_3.INSTANCE.toV10(v03StatusEvent));
        };

        Message_v0_3 message = new Message_v0_3.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();

        SendStreamingMessageRequest_v0_3 request = new SendStreamingMessageRequest_v0_3(
                "1", new MessageSendParams_v0_3(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onMessageSendStream(request, callContext);

        List<StreamingEventKind_v0_3> results = new ArrayList<>();
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
            public void onNext(SendStreamingMessageResponse_v0_3 item) {
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
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected to receive 3 events within timeout");

        // Assert no error occurred during streaming
        assertNull(error.get(), "No error should occur during streaming");

        // Verify that all 3 events were received
        assertEquals(3, results.size(), "Should have received exactly 3 events");

        // Verify the first event is the task
        Task_v0_3 receivedTask = assertInstanceOf(Task_v0_3.class, results.get(0), "First event should be a Task");
        assertEquals(MINIMAL_TASK.getId(), receivedTask.getId());
        assertEquals(MINIMAL_TASK.getContextId(), receivedTask.getContextId());
        assertEquals(TaskState_v0_3.WORKING, receivedTask.getStatus().state());

        // Verify the second event is the artifact update
        TaskArtifactUpdateEvent_v0_3 receivedArtifact = assertInstanceOf(TaskArtifactUpdateEvent_v0_3.class, results.get(1),
                "Second event should be a TaskArtifactUpdateEvent");
        assertEquals(MINIMAL_TASK.getId(), receivedArtifact.getTaskId());
        assertEquals("artifact-1", receivedArtifact.getArtifact().artifactId());

        // Verify the third event is the status update
        TaskStatusUpdateEvent_v0_3 receivedStatus = assertInstanceOf(TaskStatusUpdateEvent_v0_3.class, results.get(2),
                "Third event should be a TaskStatusUpdateEvent");
        assertEquals(MINIMAL_TASK.getId(), receivedStatus.getTaskId());
        assertEquals(TaskState_v0_3.COMPLETED, receivedStatus.getStatus().state());
    }

    @Test
    public void testOnMessageSendStreamExistingTaskSuccess() throws InterruptedException {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Configure agent to emit the task (v1.0 context contains v1.0 Task)
        agentExecutorExecute = (context, emitter) -> {
            // Emit v1.0 task - will be converted to v0.3 StreamingEventKind
            emitter.emitEvent(context.getTask());
        };

        // Save existing v0.3 task (convert to v1.0 for storage)
        Task_v0_3 v03Task = new Task_v0_3.Builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(v03Task), false);

        Message_v0_3 message = new Message_v0_3.Builder(MESSAGE)
                .taskId(v03Task.getId())
                .contextId(v03Task.getContextId())
                .build();

        SendStreamingMessageRequest_v0_3 request = new SendStreamingMessageRequest_v0_3(
                "1", new MessageSendParams_v0_3(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onMessageSendStream(request, callContext);

        // For non-final tasks, the publisher doesn't complete, so we subscribe in a new thread
        // and manually cancel after receiving the first event
        final List<StreamingEventKind_v0_3> results = new ArrayList<>();
        final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> error = new AtomicReference<>();

        Executors.newSingleThreadExecutor().execute(() -> {
            response.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriptionRef.set(subscription);
                    subscription.request(1);
                }

                @Override
                public void onNext(SendStreamingMessageResponse_v0_3 item) {
                    results.add(item.getResult());
                    subscriptionRef.get().request(1);
                    latch.countDown();
                }

                @Override
                public void onError(Throwable throwable) {
                    error.set(throwable);
                    subscriptionRef.get().cancel();
                    // Release latch to prevent timeout
                    while (latch.getCount() > 0) {
                        latch.countDown();
                    }
                }

                @Override
                public void onComplete() {
                    subscriptionRef.get().cancel();
                }
            });
        });

        // Wait for the first event
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected to receive 1 event within timeout");
        subscriptionRef.get().cancel();

        // Assert no error occurred during streaming
        assertNull(error.get(), "No error should occur during streaming");

        // Verify the task was received
        assertEquals(1, results.size(), "Should have received exactly 1 event");
        Task_v0_3 receivedTask = assertInstanceOf(Task_v0_3.class, results.get(0), "Event should be a Task");
        assertEquals(v03Task.getId(), receivedTask.getId());
        assertEquals(v03Task.getContextId(), receivedTask.getContextId());
        // Note: v1.0 backend manages task history differently than v0.3
        // The key assertion is that we received a Task event for the existing task
    }

    // ========================================
    // Streaming Error Tests
    // ========================================

    @Test
    public void testStreamingNotSupportedError() {
        // Create agent card with streaming disabled
        AgentCard_v0_3 nonStreamingCard = new AgentCard_v0_3.Builder(CARD)
                .capabilities(new AgentCapabilities_v0_3(false, true, false, null))
                .build();

        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(nonStreamingCard, internalExecutor, convert03To10Handler);

        SendStreamingMessageRequest_v0_3 request = new SendStreamingMessageRequest_v0_3(
                "1", new MessageSendParams_v0_3(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onMessageSendStream(request, callContext);

        List<SendStreamingMessageResponse_v0_3> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse_v0_3 item) {
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

        // Verify that an error response was returned
        assertEquals(1, results.size(), "Should receive exactly one error response");
        SendStreamingMessageResponse_v0_3 errorResponse = results.get(0);
        assertNotNull(errorResponse.getError(), "Response should contain an error");
        assertInstanceOf(InvalidRequestError_v0_3.class, errorResponse.getError(), "Error should be InvalidRequestError");
        assertEquals("Streaming is not supported by the agent",
                ((InvalidRequestError_v0_3) errorResponse.getError()).getMessage());
    }

    @Test
    public void testOnMessageStreamTaskIdMismatch() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to emit a task with DIFFERENT task ID than the message
        agentExecutorExecute = (context, emitter) -> {
            // Emit MINIMAL_TASK (which has different ID from MESSAGE)
            emitter.emitEvent(context.getTask());
        };

        // Send MESSAGE (which has a different task ID)
        SendStreamingMessageRequest_v0_3 request = new SendStreamingMessageRequest_v0_3(
                "1", new MessageSendParams_v0_3(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onMessageSendStream(request, callContext);

        CompletableFuture<Void> future = new CompletableFuture<>();
        List<SendStreamingMessageResponse_v0_3> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse_v0_3 item) {
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

        // Stream should complete without throwing
        assertNull(error.get(), "No exception should be thrown");

        // Should receive an error response for the task ID mismatch
        assertEquals(1, results.size(), "Should receive exactly one error response");
        SendStreamingMessageResponse_v0_3 errorResponse = results.get(0);
        assertInstanceOf(InternalError_v0_3.class, errorResponse.getError(),
                "Task ID mismatch should result in InternalError");
    }

    @Test
    public void testOnMessageStreamInternalError() {
        // Mock the Convert03To10RequestHandler to throw InternalError
        Convert_v0_3_To10RequestHandler mockedHandler = Mockito.mock(Convert_v0_3_To10RequestHandler.class);
        Mockito.doThrow(new org.a2aproject.sdk.spec.InternalError("Internal Error"))
                .when(mockedHandler)
                .onMessageSendStream(
                        Mockito.any(MessageSendParams_v0_3.class),
                        Mockito.any(ServerCallContext.class));

        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, mockedHandler);

        SendStreamingMessageRequest_v0_3 request = new SendStreamingMessageRequest_v0_3("1", new MessageSendParams_v0_3(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onMessageSendStream(request, callContext);

        List<SendStreamingMessageResponse_v0_3> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse_v0_3 item) {
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

        // Verify that an InternalError response was returned
        assertEquals(1, results.size(), "Should receive exactly one error response");
        assertInstanceOf(InternalError_v0_3.class, results.get(0).getError(), "Error should be InternalError");
    }

    @Test
    public void testStreamingNotSupportedErrorOnResubscribeToTask() {
        // Create agent card with streaming disabled
        AgentCard_v0_3 nonStreamingCard = new AgentCard_v0_3.Builder(CARD)
                .capabilities(new AgentCapabilities_v0_3(false, true, false, null))
                .build();

        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(nonStreamingCard, internalExecutor, convert03To10Handler);

        TaskResubscriptionRequest_v0_3 request = new TaskResubscriptionRequest_v0_3("1", new TaskIdParams_v0_3(MINIMAL_TASK.getId()));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onResubscribeToTask(request, callContext);

        List<SendStreamingMessageResponse_v0_3> results = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        response.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(SendStreamingMessageResponse_v0_3 item) {
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

        // Verify that an error response was returned
        assertEquals(1, results.size(), "Should receive exactly one error response");
        SendStreamingMessageResponse_v0_3 errorResponse = results.get(0);
        assertNotNull(errorResponse.getError(), "Response should contain an error");
        assertInstanceOf(InvalidRequestError_v0_3.class, errorResponse.getError(), "Error should be InvalidRequestError");
        assertEquals("Streaming is not supported by the agent",
                ((InvalidRequestError_v0_3) errorResponse.getError()).getMessage());
    }

    // ========================================
    // Push Notification Tests
    // ========================================

    @Test
    public void testSetPushNotificationConfigSuccess() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend (conversion happens internally)
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        TaskPushNotificationConfig_v0_3 taskPushConfig =
                new TaskPushNotificationConfig_v0_3(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig_v0_3.Builder()
                                .url("http://example.com")
                                .build());
        SetTaskPushNotificationConfigRequest_v0_3 request = new SetTaskPushNotificationConfigRequest_v0_3("1", taskPushConfig);
        SetTaskPushNotificationConfigResponse_v0_3 response = handler.setPushNotificationConfig(request, callContext);

        assertNull(response.getError(), "Error: " + response.getError());
        assertNotNull(response.getResult());

        TaskPushNotificationConfig_v0_3 taskPushConfigResult =
                new TaskPushNotificationConfig_v0_3(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig_v0_3.Builder()
                                .url("http://example.com")
                                .id(MINIMAL_TASK.getId())
                                .build());
        assertEquals(taskPushConfigResult, response.getResult());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig_v0_3 taskPushConfig =
                new TaskPushNotificationConfig_v0_3(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig_v0_3.Builder()
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest_v0_3 request = new SetTaskPushNotificationConfigRequest_v0_3("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        GetTaskPushNotificationConfigRequest_v0_3 getRequest =
                new GetTaskPushNotificationConfigRequest_v0_3("111", new GetTaskPushNotificationConfigParams_v0_3(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse_v0_3 getResponse = handler.getPushNotificationConfig(getRequest, callContext);

        TaskPushNotificationConfig_v0_3 expectedConfig = new TaskPushNotificationConfig_v0_3(MINIMAL_TASK.getId(),
                new PushNotificationConfig_v0_3.Builder().id(MINIMAL_TASK.getId()).url("http://example.com").build());
        assertEquals(expectedConfig, getResponse.getResult());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig_v0_3 taskPushConfig =
                new TaskPushNotificationConfig_v0_3(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig_v0_3.Builder()
                                .url("http://example.com")
                                .id(MINIMAL_TASK.getId())
                                .build());
        SetTaskPushNotificationConfigRequest_v0_3 request = new SetTaskPushNotificationConfigRequest_v0_3("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest_v0_3 deleteRequest =
                new DeleteTaskPushNotificationConfigRequest_v0_3("111", new DeleteTaskPushNotificationConfigParams_v0_3(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse_v0_3 deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getError());
        assertNull(deleteResponse.getResult());
    }

    @Test
    public void testOnGetPushNotificationNoPushNotifierConfig() {
        // Create v1.0 request handler without push config store
        org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler v10Handler =
                org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler.create(
                        agentExecutor, taskStore, queueManager, null, mainEventBusProcessor,
                        internalExecutor, internalExecutor);

        // Wrap in v0.3 conversion handler
        Convert_v0_3_To10RequestHandler handlerWithoutPushConfig = new Convert_v0_3_To10RequestHandler();
        handlerWithoutPushConfig.v10Handler = v10Handler;

        AgentCard_v0_3 card = createAgentCard(false, true, false);
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(card, internalExecutor, handlerWithoutPushConfig);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        GetTaskPushNotificationConfigRequest_v0_3 request =
                new GetTaskPushNotificationConfigRequest_v0_3("id", new GetTaskPushNotificationConfigParams_v0_3(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse_v0_3 response = handler.getPushNotificationConfig(request, callContext);

        assertNotNull(response.getError());
        assertInstanceOf(UnsupportedOperationError_v0_3.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testOnSetPushNotificationNoPushNotifierConfig() {
        // Create v1.0 request handler without push config store
        org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler v10Handler =
                org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler.create(
                        agentExecutor, taskStore, queueManager, null, mainEventBusProcessor,
                        internalExecutor, internalExecutor);

        // Wrap in v0.3 conversion handler
        Convert_v0_3_To10RequestHandler handlerWithoutPushConfig = new Convert_v0_3_To10RequestHandler();
        handlerWithoutPushConfig.v10Handler = v10Handler;

        AgentCard_v0_3 card = createAgentCard(false, true, false);
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(card, internalExecutor, handlerWithoutPushConfig);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        TaskPushNotificationConfig_v0_3 config =
                new TaskPushNotificationConfig_v0_3(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig_v0_3.Builder()
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest_v0_3 request = new SetTaskPushNotificationConfigRequest_v0_3.Builder()
                .params(config)
                .build();
        SetTaskPushNotificationConfigResponse_v0_3 response = handler.setPushNotificationConfig(request, callContext);

        assertInstanceOf(UnsupportedOperationError_v0_3.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testDeletePushNotificationConfigNotSupported() {
        AgentCard_v0_3 card = createAgentCard(true, false, false);
        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(card, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig_v0_3 taskPushConfig =
                new TaskPushNotificationConfig_v0_3(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig_v0_3.Builder()
                                .url("http://example.com")
                                .id(MINIMAL_TASK.getId())
                                .build());
        SetTaskPushNotificationConfigRequest_v0_3 request = new SetTaskPushNotificationConfigRequest_v0_3("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest_v0_3 deleteRequest =
                new DeleteTaskPushNotificationConfigRequest_v0_3("111", new DeleteTaskPushNotificationConfigParams_v0_3(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse_v0_3 deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(PushNotificationNotSupportedError_v0_3.class, deleteResponse.getError());
    }

    @Test
    public void testDeletePushNotificationConfigNoPushConfigStore() {
        // Create v1.0 request handler without push config store
        org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler v10Handler =
                org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler.create(
                        agentExecutor, taskStore, queueManager, null, mainEventBusProcessor,
                        internalExecutor, internalExecutor);

        // Wrap in v0.3 conversion handler
        Convert_v0_3_To10RequestHandler handlerWithoutPushConfig = new Convert_v0_3_To10RequestHandler();
        handlerWithoutPushConfig.v10Handler = v10Handler;

        JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, handlerWithoutPushConfig);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig_v0_3 taskPushConfig =
                new TaskPushNotificationConfig_v0_3(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig_v0_3.Builder()
                                .url("http://example.com")
                                .id(MINIMAL_TASK.getId())
                                .build());
        SetTaskPushNotificationConfigRequest_v0_3 request = new SetTaskPushNotificationConfigRequest_v0_3("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        DeleteTaskPushNotificationConfigRequest_v0_3 deleteRequest =
                new DeleteTaskPushNotificationConfigRequest_v0_3("111", new DeleteTaskPushNotificationConfigParams_v0_3(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));
        DeleteTaskPushNotificationConfigResponse_v0_3 deleteResponse =
                handler.deletePushNotificationConfig(deleteRequest, callContext);

        assertEquals("111", deleteResponse.getId());
        assertNull(deleteResponse.getResult());
        assertInstanceOf(UnsupportedOperationError_v0_3.class, deleteResponse.getError());
    }

    @Test
    public void testOnMessageStreamNewMessageSendPushNotificationSuccess() throws Exception {
        // Use synchronous executor for push notifications to ensure deterministic ordering
        // Without this, async push notifications can execute out of order, causing test flakiness
        mainEventBusProcessor.setPushNotificationExecutor(Runnable::run);

        try {
            JSONRPCHandler_v0_3 handler = new JSONRPCHandler_v0_3(CARD, internalExecutor, convert03To10Handler);

            // Save task to v1.0 backend
            org.a2aproject.sdk.spec.Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(MINIMAL_TASK);
            taskStore.save(v10Task, false);

            // Clear any previous events from httpClient
            httpClient.events.clear();

        // Create v0.3 events that the agent executor will emit
        List<Event_v0_3> events = List.of(
                MINIMAL_TASK,
                new TaskArtifactUpdateEvent_v0_3.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .artifact(new Artifact_v0_3.Builder()
                                .artifactId("11")
                                .parts(new TextPart_v0_3("text"))
                                .build())
                        .build(),
                new TaskStatusUpdateEvent_v0_3.Builder()
                        .taskId(MINIMAL_TASK.getId())
                        .contextId(MINIMAL_TASK.getContextId())
                        .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                        .isFinal(true)
                        .build());

        agentExecutorExecute = (context, agentEmitter) -> {
            // Convert v0.3 events to v1.0 and emit
            for (Event_v0_3 event : events) {
                if (event instanceof Task_v0_3) {
                    agentEmitter.emitEvent(TaskMapper_v0_3.INSTANCE.toV10((Task_v0_3) event));
                } else if (event instanceof TaskArtifactUpdateEvent_v0_3) {
                    agentEmitter.emitEvent(TaskArtifactUpdateEventMapper_v0_3.INSTANCE.toV10((TaskArtifactUpdateEvent_v0_3) event));
                } else if (event instanceof TaskStatusUpdateEvent_v0_3) {
                    agentEmitter.emitEvent(TaskStatusUpdateEventMapper_v0_3.INSTANCE.toV10((TaskStatusUpdateEvent_v0_3) event));
                }
            }
        };

        // Set push notification config
        TaskPushNotificationConfig_v0_3 config = new TaskPushNotificationConfig_v0_3(
                MINIMAL_TASK.getId(),
                new PushNotificationConfig_v0_3.Builder().url("http://example.com").build());
        SetTaskPushNotificationConfigRequest_v0_3 stpnRequest = new SetTaskPushNotificationConfigRequest_v0_3("1", config);
        SetTaskPushNotificationConfigResponse_v0_3 stpnResponse = handler.setPushNotificationConfig(stpnRequest, callContext);
        assertNull(stpnResponse.getError());

        // Send streaming message
        Message_v0_3 msg = new Message_v0_3.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .build();
        SendStreamingMessageRequest_v0_3 request = new SendStreamingMessageRequest_v0_3("1", new MessageSendParams_v0_3(msg, null, null));
        Flow.Publisher<SendStreamingMessageResponse_v0_3> response = handler.onMessageSendStream(request, callContext);

        final List<StreamingEventKind_v0_3> results = Collections.synchronizedList(new ArrayList<>());
        final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(6); // 3 streaming responses + 3 push notifications
        httpClient.latch = latch;

        Executors.newSingleThreadExecutor().execute(() -> {
            response.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriptionRef.set(subscription);
                    subscription.request(1);
                }

                @Override
                public void onNext(SendStreamingMessageResponse_v0_3 item) {
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

        boolean timedOut = !latch.await(5, TimeUnit.SECONDS);
        if (timedOut) {
            System.out.println("Test timed out! Received " + results.size() + " streaming responses, " +
                httpClient.events.size() + " push notifications. Latch count: " + latch.getCount());
            System.out.println("Push notifications received:");
            for (int i = 0; i < httpClient.events.size(); i++) {
                org.a2aproject.sdk.spec.StreamingEventKind event = httpClient.events.get(i);
                if (event instanceof org.a2aproject.sdk.spec.Task) {
                    System.out.println("  [" + i + "] Task");
                } else if (event instanceof org.a2aproject.sdk.spec.TaskArtifactUpdateEvent) {
                    System.out.println("  [" + i + "] TaskArtifactUpdateEvent");
                } else if (event instanceof org.a2aproject.sdk.spec.TaskStatusUpdateEvent) {
                    System.out.println("  [" + i + "] TaskStatusUpdateEvent");
                } else if (event instanceof org.a2aproject.sdk.spec.Message) {
                    System.out.println("  [" + i + "] Message");
                }
            }
        }
        assertTrue(!timedOut, "Test timed out waiting for events. Received " + results.size() + " streaming responses, " +
            httpClient.events.size() + " push notifications");
        subscriptionRef.get().cancel();

        // Verify streaming responses (v0.3 format)
        assertEquals(3, results.size());

        // Verify push notifications were sent (v1.0 StreamingEventKind format)
        assertEquals(3, httpClient.events.size());

        // First event: task
        org.a2aproject.sdk.spec.StreamingEventKind pushEvent0 = httpClient.events.get(0);
        assertTrue(pushEvent0 instanceof org.a2aproject.sdk.spec.Task);
        org.a2aproject.sdk.spec.Task v10PushedTask0 = (org.a2aproject.sdk.spec.Task) pushEvent0;
        assertEquals(MINIMAL_TASK.getId(), v10PushedTask0.id());
        assertEquals(MINIMAL_TASK.getContextId(), v10PushedTask0.contextId());
        // v0.3 SUBMITTED maps to v1.0 TASK_STATE_SUBMITTED
        assertEquals(org.a2aproject.sdk.spec.TaskState.TASK_STATE_SUBMITTED, v10PushedTask0.status().state());
        assertTrue(v10PushedTask0.artifacts() == null || v10PushedTask0.artifacts().isEmpty());

        // Second event: artifact update
        org.a2aproject.sdk.spec.StreamingEventKind pushEvent1 = httpClient.events.get(1);
        assertTrue(pushEvent1 instanceof org.a2aproject.sdk.spec.TaskArtifactUpdateEvent);
        org.a2aproject.sdk.spec.TaskArtifactUpdateEvent v10ArtifactUpdate = (org.a2aproject.sdk.spec.TaskArtifactUpdateEvent) pushEvent1;
        assertEquals(MINIMAL_TASK.getId(), v10ArtifactUpdate.taskId());
        assertEquals(MINIMAL_TASK.getContextId(), v10ArtifactUpdate.contextId());
        assertNotNull(v10ArtifactUpdate.artifact());
        assertEquals(1, v10ArtifactUpdate.artifact().parts().size());
        assertEquals("text", ((org.a2aproject.sdk.spec.TextPart) v10ArtifactUpdate.artifact().parts().get(0)).text());

        // Third event: status update
        org.a2aproject.sdk.spec.StreamingEventKind pushEvent2 = httpClient.events.get(2);
        assertTrue(pushEvent2 instanceof org.a2aproject.sdk.spec.TaskStatusUpdateEvent);
        org.a2aproject.sdk.spec.TaskStatusUpdateEvent v10StatusUpdate = (org.a2aproject.sdk.spec.TaskStatusUpdateEvent) pushEvent2;
        assertEquals(MINIMAL_TASK.getId(), v10StatusUpdate.taskId());
        assertEquals(MINIMAL_TASK.getContextId(), v10StatusUpdate.contextId());
        assertEquals(org.a2aproject.sdk.spec.TaskState.TASK_STATE_COMPLETED, v10StatusUpdate.status().state());
        } finally {
            // Reset push notification executor to async
            mainEventBusProcessor.setPushNotificationExecutor(null);
        }
    }

    // TODO Phase 6: Add authenticated extended card tests
}
