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

import org.a2aproject.sdk.compat03.conversion.AbstractA2ARequestHandlerTest;
import org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskArtifactUpdateEventMapper;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskMapper;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskStatusUpdateEventMapper;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

// V0.3 spec imports (client perspective)
import org.a2aproject.sdk.compat03.spec.AgentCapabilities;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.Artifact;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest;
import org.a2aproject.sdk.compat03.spec.CancelTaskResponse;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest;
import org.a2aproject.sdk.compat03.spec.GetTaskResponse;
import org.a2aproject.sdk.compat03.spec.InternalError;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError;
import org.a2aproject.sdk.compat03.spec.Message;
import org.a2aproject.sdk.compat03.spec.MessageSendParams;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest;
import org.a2aproject.sdk.compat03.spec.SendMessageResponse;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageResponse;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind;
import org.a2aproject.sdk.compat03.spec.Task;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.compat03.spec.TaskIdParams;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest;
import org.a2aproject.sdk.compat03.spec.TaskState;
import org.a2aproject.sdk.compat03.spec.TaskStatus;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.compat03.spec.TextPart;
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
    // Streaming Tests
    // ========================================

    @Test
    public void testOnMessageSendStreamSuccess() throws InterruptedException {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Configure agent to emit the message back (v1.0 context contains v1.0 Message)
        agentExecutorExecute = (context, emitter) -> {
            // Emit v1.0 message - will be converted to v0.3 StreamingEventKind
            emitter.emitEvent(context.getMessage());
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

        // Wait for event to be received
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected to receive 1 event within timeout");

        // Assert no error occurred during streaming
        assertNull(error.get(), "No error should occur during streaming");

        // Verify that the message was received
        assertEquals(1, results.size(), "Should have received exactly 1 event");

        // Verify the event is the message (converted back from v1.0)
        Message receivedMessage = assertInstanceOf(Message.class, results.get(0), "Event should be a Message");
        assertEquals(message.getMessageId(), receivedMessage.getMessageId());
    }

    @Test
    public void testOnMessageSendStreamMultipleEventsSuccess() throws InterruptedException {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Create v0.3 events for reference (we'll emit v1.0 equivalents)
        Task v03TaskEvent = new Task.Builder(MINIMAL_TASK)
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        TaskArtifactUpdateEvent v03ArtifactEvent = new TaskArtifactUpdateEvent.Builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .artifact(new Artifact.Builder()
                        .artifactId("artifact-1")
                        .parts(new TextPart("Generated artifact content"))
                        .build())
                .build();

        TaskStatusUpdateEvent v03StatusEvent = new TaskStatusUpdateEvent.Builder()
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true) // Must be true for COMPLETED state in v1.0
                .build();

        // Configure the agent executor to emit multiple v1.0 events
        agentExecutorExecute = (context, emitter) -> {
            // Convert v0.3 events to v1.0 and emit them
            // The emitter will convert them back to v0.3 StreamingEventKind for the response
            emitter.emitEvent(TaskMapper.INSTANCE.toV10(v03TaskEvent));
            emitter.emitEvent(TaskArtifactUpdateEventMapper.INSTANCE.toV10(v03ArtifactEvent));
            emitter.emitEvent(TaskStatusUpdateEventMapper.INSTANCE.toV10(v03StatusEvent));
        };

        Message message = new Message.Builder(MESSAGE)
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
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Expected to receive 3 events within timeout");

        // Assert no error occurred during streaming
        assertNull(error.get(), "No error should occur during streaming");

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
    public void testOnMessageSendStreamExistingTaskSuccess() throws InterruptedException {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Configure agent to emit the task (v1.0 context contains v1.0 Task)
        agentExecutorExecute = (context, emitter) -> {
            // Emit v1.0 task - will be converted to v0.3 StreamingEventKind
            emitter.emitEvent(context.getTask());
        };

        // Save existing v0.3 task (convert to v1.0 for storage)
        Task v03Task = new Task.Builder(MINIMAL_TASK)
                .history(new ArrayList<>())
                .build();
        taskStore.save(TaskMapper.INSTANCE.toV10(v03Task), false);

        Message message = new Message.Builder(MESSAGE)
                .taskId(v03Task.getId())
                .contextId(v03Task.getContextId())
                .build();

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        // For non-final tasks, the publisher doesn't complete, so we subscribe in a new thread
        // and manually cancel after receiving the first event
        final List<StreamingEventKind> results = new ArrayList<>();
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
                public void onNext(SendStreamingMessageResponse item) {
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
        Task receivedTask = assertInstanceOf(Task.class, results.get(0), "Event should be a Task");
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
        AgentCard nonStreamingCard = new AgentCard.Builder(CARD)
                .capabilities(new AgentCapabilities(false, true, false, null))
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(nonStreamingCard, internalExecutor, convert03To10Handler);

        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

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

        // Verify that an error response was returned
        assertEquals(1, results.size(), "Should receive exactly one error response");
        SendStreamingMessageResponse errorResponse = results.get(0);
        assertNotNull(errorResponse.getError(), "Response should contain an error");
        assertInstanceOf(InvalidRequestError.class, errorResponse.getError(), "Error should be InvalidRequestError");
        assertEquals("Streaming is not supported by the agent",
                ((InvalidRequestError) errorResponse.getError()).getMessage());
    }

    @Test
    public void testOnMessageStreamTaskIdMismatch() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save existing task
        taskStore.save(TaskMapper.INSTANCE.toV10(MINIMAL_TASK), false);

        // Configure agent to emit a task with DIFFERENT task ID than the message
        agentExecutorExecute = (context, emitter) -> {
            // Emit MINIMAL_TASK (which has different ID from MESSAGE)
            emitter.emitEvent(context.getTask());
        };

        // Send MESSAGE (which has a different task ID)
        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        CompletableFuture<Void> future = new CompletableFuture<>();
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
        SendStreamingMessageResponse errorResponse = results.get(0);
        assertInstanceOf(InternalError.class, errorResponse.getError(),
                "Task ID mismatch should result in InternalError");
    }

    @Test
    public void testOnMessageStreamInternalError() {
        // Mock the Convert03To10RequestHandler to throw InternalError
        Convert03To10RequestHandler mockedHandler = Mockito.mock(Convert03To10RequestHandler.class);
        Mockito.doThrow(new org.a2aproject.sdk.spec.InternalError("Internal Error"))
                .when(mockedHandler)
                .onMessageSendStream(
                        Mockito.any(org.a2aproject.sdk.compat03.spec.MessageSendParams.class),
                        Mockito.any(ServerCallContext.class));

        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, mockedHandler);

        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(MESSAGE, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

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

        // Verify that an InternalError response was returned
        assertEquals(1, results.size(), "Should receive exactly one error response");
        assertInstanceOf(InternalError.class, results.get(0).getError(), "Error should be InternalError");
    }

    @Test
    public void testStreamingNotSupportedErrorOnResubscribeToTask() {
        // Create agent card with streaming disabled
        AgentCard nonStreamingCard = new AgentCard.Builder(CARD)
                .capabilities(new AgentCapabilities(false, true, false, null))
                .build();

        JSONRPCHandler handler = new JSONRPCHandler(nonStreamingCard, internalExecutor, convert03To10Handler);

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

        // Verify that an error response was returned
        assertEquals(1, results.size(), "Should receive exactly one error response");
        SendStreamingMessageResponse errorResponse = results.get(0);
        assertNotNull(errorResponse.getError(), "Response should contain an error");
        assertInstanceOf(InvalidRequestError.class, errorResponse.getError(), "Error should be InvalidRequestError");
        assertEquals("Streaming is not supported by the agent",
                ((InvalidRequestError) errorResponse.getError()).getMessage());
    }

    // ========================================
    // Push Notification Tests
    // ========================================

    @Test
    public void testSetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend (conversion happens internally)
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
                                .url("http://example.com")
                                .build());
        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        SetTaskPushNotificationConfigResponse response = handler.setPushNotificationConfig(request, callContext);

        assertNull(response.getError(), "Error: " + response.getError());
        assertNotNull(response.getResult());

        TaskPushNotificationConfig taskPushConfigResult =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
                                .url("http://example.com")
                                .id(MINIMAL_TASK.getId())
                                .build());
        assertEquals(taskPushConfigResult, response.getResult());
    }

    @Test
    public void testGetPushNotificationConfigSuccess() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
                                .url("http://example.com")
                                .build());

        SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
        handler.setPushNotificationConfig(request, callContext);

        GetTaskPushNotificationConfigRequest getRequest =
                new GetTaskPushNotificationConfigRequest("111", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse getResponse = handler.getPushNotificationConfig(getRequest, callContext);

        TaskPushNotificationConfig expectedConfig = new TaskPushNotificationConfig(MINIMAL_TASK.getId(),
                new PushNotificationConfig.Builder().id(MINIMAL_TASK.getId()).url("http://example.com").build());
        assertEquals(expectedConfig, getResponse.getResult());
    }

    @Test
    public void testDeletePushNotificationConfig() {
        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
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
    public void testOnGetPushNotificationNoPushNotifierConfig() {
        // Create v1.0 request handler without push config store
        org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler v10Handler =
                org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler.create(
                        agentExecutor, taskStore, queueManager, null, mainEventBusProcessor,
                        internalExecutor, internalExecutor);

        // Wrap in v0.3 conversion handler
        Convert03To10RequestHandler handlerWithoutPushConfig = new Convert03To10RequestHandler();
        handlerWithoutPushConfig.v10Handler = v10Handler;

        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, internalExecutor, handlerWithoutPushConfig);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        GetTaskPushNotificationConfigRequest request =
                new GetTaskPushNotificationConfigRequest("id", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
        GetTaskPushNotificationConfigResponse response = handler.getPushNotificationConfig(request, callContext);

        assertNotNull(response.getError());
        assertInstanceOf(UnsupportedOperationError.class, response.getError());
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
        Convert03To10RequestHandler handlerWithoutPushConfig = new Convert03To10RequestHandler();
        handlerWithoutPushConfig.v10Handler = v10Handler;

        AgentCard card = createAgentCard(false, true, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, internalExecutor, handlerWithoutPushConfig);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

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

        assertInstanceOf(UnsupportedOperationError.class, response.getError());
        assertEquals("This operation is not supported", response.getError().getMessage());
    }

    @Test
    public void testDeletePushNotificationConfigNotSupported() {
        AgentCard card = createAgentCard(true, false, false);
        JSONRPCHandler handler = new JSONRPCHandler(card, internalExecutor, convert03To10Handler);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
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
        // Create v1.0 request handler without push config store
        org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler v10Handler =
                org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler.create(
                        agentExecutor, taskStore, queueManager, null, mainEventBusProcessor,
                        internalExecutor, internalExecutor);

        // Wrap in v0.3 conversion handler
        Convert03To10RequestHandler handlerWithoutPushConfig = new Convert03To10RequestHandler();
        handlerWithoutPushConfig.v10Handler = v10Handler;

        JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, handlerWithoutPushConfig);

        // Save task to v1.0 backend
        org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
        taskStore.save(v10Task, false);

        agentExecutorExecute = (context, agentEmitter) -> {
            agentEmitter.emitEvent(context.getTask() != null ? context.getTask() : context.getMessage());
        };

        TaskPushNotificationConfig taskPushConfig =
                new TaskPushNotificationConfig(
                        MINIMAL_TASK.getId(),
                        new PushNotificationConfig.Builder()
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
    public void testOnMessageStreamNewMessageSendPushNotificationSuccess() throws Exception {
        // Use synchronous executor for push notifications to ensure deterministic ordering
        // Without this, async push notifications can execute out of order, causing test flakiness
        mainEventBusProcessor.setPushNotificationExecutor(Runnable::run);

        try {
            JSONRPCHandler handler = new JSONRPCHandler(CARD, internalExecutor, convert03To10Handler);

            // Save task to v1.0 backend
            org.a2aproject.sdk.spec.Task v10Task = TaskMapper.INSTANCE.toV10(MINIMAL_TASK);
            taskStore.save(v10Task, false);

            // Clear any previous events from httpClient
            httpClient.events.clear();

        // Create v0.3 events that the agent executor will emit
        List<org.a2aproject.sdk.compat03.spec.Event> events = List.of(
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
                        .isFinal(true)
                        .build());

        agentExecutorExecute = (context, agentEmitter) -> {
            // Convert v0.3 events to v1.0 and emit
            for (org.a2aproject.sdk.compat03.spec.Event event : events) {
                if (event instanceof Task) {
                    agentEmitter.emitEvent(TaskMapper.INSTANCE.toV10((Task) event));
                } else if (event instanceof TaskArtifactUpdateEvent) {
                    agentEmitter.emitEvent(TaskArtifactUpdateEventMapper.INSTANCE.toV10((TaskArtifactUpdateEvent) event));
                } else if (event instanceof TaskStatusUpdateEvent) {
                    agentEmitter.emitEvent(TaskStatusUpdateEventMapper.INSTANCE.toV10((TaskStatusUpdateEvent) event));
                }
            }
        };

        // Set push notification config
        TaskPushNotificationConfig config = new TaskPushNotificationConfig(
                MINIMAL_TASK.getId(),
                new PushNotificationConfig.Builder().url("http://example.com").build());
        SetTaskPushNotificationConfigRequest stpnRequest = new SetTaskPushNotificationConfigRequest("1", config);
        SetTaskPushNotificationConfigResponse stpnResponse = handler.setPushNotificationConfig(stpnRequest, callContext);
        assertNull(stpnResponse.getError());

        // Send streaming message
        Message msg = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest("1", new MessageSendParams(msg, null, null));
        Flow.Publisher<SendStreamingMessageResponse> response = handler.onMessageSendStream(request, callContext);

        final List<StreamingEventKind> results = Collections.synchronizedList(new ArrayList<>());
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
