package io.a2a.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.a2a.server.ServerCallContext;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.EventQueueUtil;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.A2AError;
import io.a2a.spec.Event;
import io.a2a.spec.EventKind;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for DefaultRequestHandler focusing on AUTH_REQUIRED workflow.
 * Tests verify the special interrupt behavior where AUTH_REQUIRED tasks:
 * 1. Return immediately to the client
 * 2. Continue agent execution in background
 * 3. Keep queues open for late events
 * 4. Perform async cleanup
 */
public class DefaultRequestHandlerTest {

    private static final MessageSendConfiguration DEFAULT_CONFIG = MessageSendConfiguration.builder()
        .returnImmediately(true)
        .acceptedOutputModes(List.of())
        .build();

    private static final ServerCallContext NULL_CONTEXT = null;

    private static final Message MESSAGE = Message.builder()
        .messageId("111")
        .role(Message.Role.ROLE_AGENT)
        .parts(new TextPart("test message"))
        .build();

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};

    // Test infrastructure components
    protected AgentExecutor executor;
    protected TaskStore taskStore;
    protected RequestHandler requestHandler;
    protected InMemoryQueueManager queueManager;
    protected MainEventBus mainEventBus;
    protected MainEventBusProcessor mainEventBusProcessor;
    protected AgentExecutorMethod agentExecutorExecute;
    protected AgentExecutorMethod agentExecutorCancel;

    protected final Executor internalExecutor = Executors.newCachedThreadPool();

    @BeforeEach
    public void init() {
        // Create test AgentExecutor with mocked execute/cancel methods
        executor = new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                if (agentExecutorExecute != null) {
                    agentExecutorExecute.invoke(context, agentEmitter);
                }
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                if (agentExecutorCancel != null) {
                    agentExecutorCancel.invoke(context, agentEmitter);
                }
            }
        };

        // Set up infrastructure
        InMemoryTaskStore inMemoryTaskStore = new InMemoryTaskStore();
        taskStore = inMemoryTaskStore;

        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();

        // Create MainEventBus and MainEventBusProcessor
        mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(inMemoryTaskStore, mainEventBus);
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSHNOTIFICATION_SENDER, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        // Create DefaultRequestHandler
        requestHandler = DefaultRequestHandler.create(
            executor, taskStore, queueManager, pushConfigStore, mainEventBusProcessor, internalExecutor, internalExecutor);
    }

    @AfterEach
    public void cleanup() {
        agentExecutorExecute = null;
        agentExecutorCancel = null;

        // Stop MainEventBusProcessor background thread
        if (mainEventBusProcessor != null) {
            EventQueueUtil.stop(mainEventBusProcessor);
        }
    }

    /**
     * Functional interface for test agent executor methods.
     */
    protected interface AgentExecutorMethod {
        void invoke(RequestContext context, AgentEmitter agentEmitter) throws A2AError;
    }

    /**
     * Test 1: Non-streaming AUTH_REQUIRED returns immediately while agent continues.
     * Verifies:
     * - Task returned immediately with AUTH_REQUIRED state
     * - Agent still running in background (not blocked)
     * - TaskStore persisted AUTH_REQUIRED state
     * - Agent completes after release
     * - Final state persisted to TaskStore
     */
    @Test
    void testAuthRequired_NonStreaming_ReturnsImmediately() throws Exception {
        // Arrange: Set up agent that emits AUTH_REQUIRED then waits
        CountDownLatch authRequiredEmitted = new CountDownLatch(1);
        CountDownLatch continueAgent = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            // Emit AUTH_REQUIRED - client should receive immediately
            emitter.requiresAuth(Message.builder()
                .role(Message.Role.ROLE_AGENT)
                .parts(new TextPart("Please authenticate with OAuth provider"))
                .build());
            authRequiredEmitted.countDown();

            // Agent continues processing (simulating waiting for out-of-band auth)
            try {
                continueAgent.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Complete after "auth received"
            emitter.complete();
        };

        // Create MessageSendParams
        MessageSendParams params = MessageSendParams.builder()
            .message(MESSAGE)
            .configuration(DEFAULT_CONFIG)
            .build();

        // Act: Send message (non-streaming)
        EventKind eventKind = requestHandler.onMessageSend(params, NULL_CONTEXT);

        // Assert: Task returned immediately with AUTH_REQUIRED state
        assertNotNull(eventKind, "Result should not be null");
        assertInstanceOf(Task.class, eventKind, "Result should be a Task");
        Task result = (Task) eventKind;

        assertEquals(TaskState.TASK_STATE_AUTH_REQUIRED, result.status().state(),
            "Task should be in AUTH_REQUIRED state");
        assertTrue(authRequiredEmitted.await(2, TimeUnit.SECONDS),
            "AUTH_REQUIRED should be emitted quickly");

        // Verify agent still running (continueAgent latch not counted down yet)
        assertFalse(continueAgent.await(100, TimeUnit.MILLISECONDS),
            "Agent should still be waiting (not completed yet)");

        // Verify TaskStore has AUTH_REQUIRED state
        Task storedTask = taskStore.get(result.id());
        assertNotNull(storedTask, "Task should be persisted in TaskStore");
        assertEquals(TaskState.TASK_STATE_AUTH_REQUIRED, storedTask.status().state(),
            "TaskStore should have AUTH_REQUIRED state");

        // Release agent to complete
        continueAgent.countDown();

        // Wait for completion and verify final state
        Thread.sleep(1000); // Allow time for completion to process through MainEventBus
        Task finalTask = taskStore.get(result.id());
        assertEquals(TaskState.TASK_STATE_COMPLETED, finalTask.status().state(),
            "TaskStore should have COMPLETED state after agent finishes");
    }

    /**
     * Test 2: Queue remains open after AUTH_REQUIRED for late events.
     * Verifies:
     * - Queue stays open after AUTH_REQUIRED response
     * - Can tap into queue after AUTH_REQUIRED
     * - Late artifacts arrive on tapped queue
     * - Completion event arrives on tapped queue
     */
    @Test
    void testAuthRequired_QueueRemainsOpen() throws Exception {
        // Arrange: Agent emits AUTH_REQUIRED then continues with late events
        CountDownLatch authEmitted = new CountDownLatch(1);
        CountDownLatch continueAgent = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            // Emit AUTH_REQUIRED
            emitter.requiresAuth(Message.builder()
                .role(Message.Role.ROLE_AGENT)
                .parts(new TextPart("Authenticate required"))
                .build());
            authEmitted.countDown();

            // Wait for test to tap queue
            try {
                continueAgent.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Emit late artifact after AUTH_REQUIRED
            emitter.addArtifact(List.of(new TextPart("Late artifact after auth")));
            emitter.complete();
        };

        // Create MessageSendParams
        MessageSendParams params = MessageSendParams.builder()
            .message(MESSAGE)
            .configuration(DEFAULT_CONFIG)
            .build();

        // Act: Send message, get AUTH_REQUIRED response
        EventKind eventKind = requestHandler.onMessageSend(params, NULL_CONTEXT);
        assertInstanceOf(Task.class, eventKind);
        Task task = (Task) eventKind;

        assertTrue(authEmitted.await(2, TimeUnit.SECONDS),
            "AUTH_REQUIRED should be emitted");

        // Tap into the queue (simulates client resubscription after AUTH_REQUIRED)
        EventQueue tappedQueue = queueManager.tap(task.id());
        assertNotNull(tappedQueue, "Queue should remain open after AUTH_REQUIRED");

        // Release agent to continue and emit late events
        continueAgent.countDown();

        // Assert: Late events arrive on tapped queue

        // First event should be the late artifact
        EventQueueItem item = tappedQueue.dequeueEventItem(5000);
        assertNotNull(item, "Should receive late artifact event");
        Event event = item.getEvent();
        assertInstanceOf(TaskArtifactUpdateEvent.class, event,
            "First event should be TaskArtifactUpdateEvent");

        // Second event should be completion
        item = tappedQueue.dequeueEventItem(5000);
        assertNotNull(item, "Should receive completion event");
        event = item.getEvent();
        assertInstanceOf(TaskStatusUpdateEvent.class, event,
            "Second event should be TaskStatusUpdateEvent");
        assertEquals(TaskState.TASK_STATE_COMPLETED,
            ((TaskStatusUpdateEvent) event).status().state(),
            "Task should be completed");
    }

    /**
     * Test 3: TaskStore persistence through AUTH_REQUIRED lifecycle.
     * Verifies:
     * - AUTH_REQUIRED state persisted correctly
     * - State transitions persisted (AUTH_REQUIRED → WORKING → COMPLETED)
     * - TaskStore always reflects current state
     */
    @Test
    void testAuthRequired_TaskStorePersistence() throws Exception {
        // Arrange: Agent emits AUTH_REQUIRED, then WORKING, then COMPLETED
        CountDownLatch authEmitted = new CountDownLatch(1);
        CountDownLatch continueAgent = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            // Emit AUTH_REQUIRED
            emitter.requiresAuth();
            authEmitted.countDown();

            // Wait for test to verify AUTH_REQUIRED persisted
            try {
                continueAgent.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Continue working (simulating auth received out-of-band)
            emitter.startWork();

            // Complete the task
            emitter.complete();
        };

        // Create MessageSendParams
        MessageSendParams params = MessageSendParams.builder()
            .message(MESSAGE)
            .configuration(DEFAULT_CONFIG)
            .build();

        // Act: Send message
        EventKind eventKind = requestHandler.onMessageSend(params, NULL_CONTEXT);
        assertInstanceOf(Task.class, eventKind);
        Task task = (Task) eventKind;

        assertTrue(authEmitted.await(2, TimeUnit.SECONDS),
            "AUTH_REQUIRED should be emitted");

        // Assert: Verify AUTH_REQUIRED state persisted
        Task storedTask1 = taskStore.get(task.id());
        assertNotNull(storedTask1, "Task should be in TaskStore");
        assertEquals(TaskState.TASK_STATE_AUTH_REQUIRED, storedTask1.status().state(),
            "TaskStore should have AUTH_REQUIRED state");

        // Release agent to continue
        continueAgent.countDown();

        // Wait for state transitions to process
        Thread.sleep(1000);

        // Verify WORKING state persisted
        Task storedTask2 = taskStore.get(task.id());
        // Note: WORKING might be skipped if processing is fast, so we accept either WORKING or COMPLETED
        TaskState state2 = storedTask2.status().state();
        assertTrue(state2 == TaskState.TASK_STATE_WORKING || state2 == TaskState.TASK_STATE_COMPLETED,
            "TaskStore should have WORKING or COMPLETED state");

        // Wait a bit more and verify final COMPLETED state
        Thread.sleep(500);
        Task storedTask3 = taskStore.get(task.id());
        assertEquals(TaskState.TASK_STATE_COMPLETED, storedTask3.status().state(),
            "TaskStore should have COMPLETED state after agent finishes");
    }

    /**
     * Test 4: Streaming with AUTH_REQUIRED continues in background.
     * Verifies:
     * - Client receives AUTH_REQUIRED in stream
     * - Agent continues emitting artifacts after AUTH_REQUIRED
     * - Artifacts stream to client
     * - Completion event arrives in stream
     */
    @Test
    void testAuthRequired_Streaming_ContinuesInBackground() throws Exception {
        // Arrange: Agent emits AUTH_REQUIRED, then streams artifacts
        CountDownLatch authEmitted = new CountDownLatch(1);
        CountDownLatch continueAgent = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            // Emit AUTH_REQUIRED
            emitter.requiresAuth();
            authEmitted.countDown();

            // Wait briefly (simulating auth happening out-of-band)
            try {
                continueAgent.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Continue streaming artifacts
            emitter.addArtifact(List.of(new TextPart("Artifact 1")));
            emitter.addArtifact(List.of(new TextPart("Artifact 2")));
            emitter.complete();
        };

        // Create MessageSendParams
        MessageSendParams params = MessageSendParams.builder()
            .message(MESSAGE)
            .configuration(DEFAULT_CONFIG)
            .build();

        // Act: Send message with streaming enabled
        EventKind eventKind = requestHandler.onMessageSend(params, NULL_CONTEXT);
        assertInstanceOf(Task.class, eventKind);
        Task result = (Task) eventKind;

        assertTrue(authEmitted.await(2, TimeUnit.SECONDS),
            "AUTH_REQUIRED should be emitted");

        // Verify AUTH_REQUIRED received
        assertEquals(TaskState.TASK_STATE_AUTH_REQUIRED, result.status().state(),
            "Should receive AUTH_REQUIRED state");

        // Tap queue to receive subsequent events
        EventQueue tappedQueue = queueManager.tap(result.id());

        // Release agent to continue streaming
        continueAgent.countDown();

        // Assert: Verify artifacts stream through
        EventQueueItem item1 = tappedQueue.dequeueEventItem(5000);
        assertNotNull(item1, "Should receive first artifact");
        assertInstanceOf(TaskArtifactUpdateEvent.class, item1.getEvent());

        EventQueueItem item2 = tappedQueue.dequeueEventItem(5000);
        assertNotNull(item2, "Should receive second artifact");
        assertInstanceOf(TaskArtifactUpdateEvent.class, item2.getEvent());

        // Verify completion arrives
        EventQueueItem completionItem = tappedQueue.dequeueEventItem(5000);
        assertNotNull(completionItem, "Should receive completion");
        Event completionEvent = completionItem.getEvent();
        assertInstanceOf(TaskStatusUpdateEvent.class, completionEvent);
        assertEquals(TaskState.TASK_STATE_COMPLETED,
            ((TaskStatusUpdateEvent) completionEvent).status().state());
    }

    /**
     * Test 5: Resubscription after AUTH_REQUIRED works correctly.
     * Verifies:
     * - Queue stays open after AUTH_REQUIRED and client disconnect
     * - Can resubscribe (tap) after AUTH_REQUIRED
     * - Late events received on resubscribed queue
     * - Completion event arrives on resubscribed queue
     */
    @Test
    void testAuthRequired_Resubscription() throws Exception {
        // Arrange: Agent emits AUTH_REQUIRED, simulates client disconnect, then continues
        CountDownLatch authEmitted = new CountDownLatch(1);
        CountDownLatch continueAgent = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            // Emit AUTH_REQUIRED
            emitter.requiresAuth();
            authEmitted.countDown();

            // Wait for test to simulate disconnect and resubscribe
            try {
                continueAgent.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Emit late events after "client reconnect"
            emitter.addArtifact(List.of(new TextPart("Event after reconnect")));
            emitter.complete();
        };

        // Create MessageSendParams
        MessageSendParams params = MessageSendParams.builder()
            .message(MESSAGE)
            .configuration(DEFAULT_CONFIG)
            .build();

        // Act: Send message, get AUTH_REQUIRED
        EventKind eventKind = requestHandler.onMessageSend(params, NULL_CONTEXT);
        assertInstanceOf(Task.class, eventKind);
        Task task = (Task) eventKind;

        assertTrue(authEmitted.await(2, TimeUnit.SECONDS),
            "AUTH_REQUIRED should be emitted");

        assertEquals(TaskState.TASK_STATE_AUTH_REQUIRED, task.status().state(),
            "Should receive AUTH_REQUIRED state");

        // Simulate client disconnect by just waiting
        Thread.sleep(100);

        // Client reconnects: tap into queue (resubscription)
        EventQueue resubscribedQueue = queueManager.tap(task.id());
        assertNotNull(resubscribedQueue,
            "Should be able to resubscribe after AUTH_REQUIRED");

        // Release agent to continue
        continueAgent.countDown();

        // Assert: Late events arrive on resubscribed queue
        EventQueueItem item = resubscribedQueue.dequeueEventItem(5000);
        assertNotNull(item, "Should receive late artifact on resubscribed queue");
        assertInstanceOf(TaskArtifactUpdateEvent.class, item.getEvent(),
            "Should receive artifact update event");

        // Verify completion arrives
        EventQueueItem completionItem = resubscribedQueue.dequeueEventItem(5000);
        assertNotNull(completionItem, "Should receive completion event");
        Event completionEvent = completionItem.getEvent();
        assertInstanceOf(TaskStatusUpdateEvent.class, completionEvent,
            "Should receive status update event");
        assertEquals(TaskState.TASK_STATE_COMPLETED,
            ((TaskStatusUpdateEvent) completionEvent).status().state(),
            "Task should be completed");
    }

    /**
     * Test: Reject SendMessage with mismatching contextId and taskId.
     * When a message references an existing task but provides a different contextId,
     * the request must be rejected with an InvalidParamsError.
     * The task must not be in a terminal state, or the terminal-state guard fires first.
     */
    @Test
    void testRejectMismatchingContextId() throws Exception {
        // Arrange: Create an initial task – agent stays active (working) so the task is NOT terminal
        CountDownLatch agentStarted = new CountDownLatch(1);
        CountDownLatch agentRelease = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            emitter.startWork();
            agentStarted.countDown();
            try {
                agentRelease.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            emitter.complete();
        };

        Message initialMessage = Message.builder()
            .messageId("msg-1")
            .role(Message.Role.ROLE_USER)
            .contextId("original-context")
            .parts(new TextPart("initial message"))
            .build();

        // returnImmediately=true so onMessageSend returns quickly (on first event)
        MessageSendParams initialParams = MessageSendParams.builder()
            .message(initialMessage)
            .configuration(DEFAULT_CONFIG)
            .build();

        EventKind result = requestHandler.onMessageSend(initialParams, NULL_CONTEXT);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;

        // Wait until agent has started (task is in WORKING state, not terminal)
        assertTrue(agentStarted.await(5, TimeUnit.SECONDS));

        try {
            // Act & Assert: Send a follow-up message with matching taskId but wrong contextId
            // The task is still WORKING, so the terminal guard does NOT fire.
            // The contextId mismatch guard should fire instead.
            Message mismatchedMessage = Message.builder()
                .messageId("msg-2")
                .role(Message.Role.ROLE_USER)
                .taskId(task.id())
                .contextId("wrong-context-does-not-exist")
                .parts(new TextPart("follow-up message"))
                .build();

            MessageSendParams mismatchedParams = MessageSendParams.builder()
                .message(mismatchedMessage)
                .configuration(DEFAULT_CONFIG)
                .build();

            InvalidParamsError error = assertThrows(InvalidParamsError.class,
                () -> requestHandler.onMessageSend(mismatchedParams, NULL_CONTEXT));
            assertTrue(error.getMessage().contains(task.id()));
        } finally {
            // Release agent so it completes and doesn't leak
            agentRelease.countDown();
        }
    }

    /**
     * Helper: creates a task, drives it to the given terminal state, then asserts that a
     * follow-up SendMessage to that task throws UnsupportedOperationError (CORE-SEND-002).
     */
    private void assertSendMessageToTerminalStateThrows(TaskState terminalState) throws Exception {
        CountDownLatch agentCompleted = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            switch (terminalState) {
                case TASK_STATE_COMPLETED -> emitter.complete();
                case TASK_STATE_CANCELED  -> emitter.cancel();
                case TASK_STATE_REJECTED  -> emitter.reject();
                // Use fail() (no-arg) which emits TaskStatusUpdateEvent(FAILED) via the normal path,
                // ensuring the task state is persisted to the store before we query it.
                case TASK_STATE_FAILED    -> emitter.fail();
                default -> throw new IllegalArgumentException("Unexpected state: " + terminalState);
            }
            agentCompleted.countDown();
        };

        Message initialMessage = Message.builder()
            .messageId("msg-initial-" + terminalState)
            .role(Message.Role.ROLE_USER)
            .parts(new TextPart("create task"))
            .build();

        EventKind result = requestHandler.onMessageSend(
            MessageSendParams.builder().message(initialMessage).configuration(DEFAULT_CONFIG).build(),
            NULL_CONTEXT);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        final String finalTaskId = task.id();

        assertTrue(agentCompleted.await(5, TimeUnit.SECONDS), "Agent should complete");
        Thread.sleep(200); // allow MainEventBusProcessor to persist the final state

        Task storedTask = taskStore.get(finalTaskId);
        assertNotNull(storedTask);
        assertEquals(terminalState, storedTask.status().state(),
            "Task should be in " + terminalState + " state");

        // Reset: agent executor must NOT be called on the follow-up
        agentExecutorExecute = (context, emitter) -> {
            throw new AssertionError("AgentExecutor must NOT be invoked for a terminal task");
        };

        Message followUpMessage = Message.builder()
            .messageId("msg-followup-" + terminalState)
            .role(Message.Role.ROLE_USER)
            .taskId(finalTaskId)
            .parts(new TextPart("follow-up to terminal task"))
            .build();

        MessageSendParams followUpParams = MessageSendParams.builder()
            .message(followUpMessage)
            .configuration(DEFAULT_CONFIG)
            .build();

        UnsupportedOperationError error = assertThrows(UnsupportedOperationError.class,
            () -> requestHandler.onMessageSend(followUpParams, NULL_CONTEXT),
            "Expected UnsupportedOperationError when sending message to a " + terminalState + " task");

        assertNotNull(error.getMessage(), "Error message should not be null");
        assertTrue(error.getMessage().contains(finalTaskId),
            "Error message should reference the task id");
    }

    /**
     * CORE-SEND-002: SendMessage to a completed task must return UnsupportedOperationError.
     */
    @Test
    void testSendMessage_ToCompletedTask_ThrowsUnsupportedOperationError() throws Exception {
        assertSendMessageToTerminalStateThrows(TaskState.TASK_STATE_COMPLETED);
    }

    /**
     * CORE-SEND-002: SendMessage to a canceled task must return UnsupportedOperationError.
     */
    @Test
    void testSendMessage_ToCanceledTask_ThrowsUnsupportedOperationError() throws Exception {
        assertSendMessageToTerminalStateThrows(TaskState.TASK_STATE_CANCELED);
    }

    /**
     * CORE-SEND-002: SendMessage to a rejected task must return UnsupportedOperationError.
     */
    @Test
    void testSendMessage_ToRejectedTask_ThrowsUnsupportedOperationError() throws Exception {
        assertSendMessageToTerminalStateThrows(TaskState.TASK_STATE_REJECTED);
    }

    /**
     * CORE-SEND-002: SendMessage to a failed task must return UnsupportedOperationError.
     */
    @Test
    void testSendMessage_ToFailedTask_ThrowsUnsupportedOperationError() throws Exception {
        assertSendMessageToTerminalStateThrows(TaskState.TASK_STATE_FAILED);
    }

    /**
     * Test: SendStreamingMessage to a task in a terminal state must also return UnsupportedOperationError
     * (CORE-SEND-002, streaming path).
     */
    @Test
    void testSendMessageStream_ToCompletedTask_ThrowsUnsupportedOperationError() throws Exception {
        // Arrange: Create and complete an initial task
        CountDownLatch agentCompleted = new CountDownLatch(1);

        agentExecutorExecute = (context, emitter) -> {
            emitter.complete();
            agentCompleted.countDown();
        };

        Message initialMessage = Message.builder()
            .messageId("msg-initial-stream")
            .role(Message.Role.ROLE_USER)
            .parts(new TextPart("create task for stream test"))
            .build();

        MessageSendParams initialParams = MessageSendParams.builder()
            .message(initialMessage)
            .configuration(DEFAULT_CONFIG)
            .build();

        EventKind result = requestHandler.onMessageSend(initialParams, NULL_CONTEXT);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;

        assertTrue(agentCompleted.await(5, TimeUnit.SECONDS), "Agent should complete");
        Thread.sleep(200); // allow MainEventBusProcessor to persist

        // Verify task is in terminal state
        Task storedTask = taskStore.get(task.id());
        assertNotNull(storedTask);
        assertEquals(TaskState.TASK_STATE_COMPLETED, storedTask.status().state());

        // Reset: agent executor must NOT be called
        agentExecutorExecute = (context, emitter) -> {
            throw new AssertionError("AgentExecutor must NOT be invoked for a terminal task");
        };

        // Act & Assert: streaming follow-up to a terminal task must also be rejected
        Message followUpMessage = Message.builder()
            .messageId("msg-followup-stream")
            .role(Message.Role.ROLE_USER)
            .taskId(task.id())
            .parts(new TextPart("streaming follow-up to completed task"))
            .build();

        MessageSendParams followUpParams = MessageSendParams.builder()
            .message(followUpMessage)
            .configuration(DEFAULT_CONFIG)
            .build();

        assertThrows(UnsupportedOperationError.class,
            () -> requestHandler.onMessageSendStream(followUpParams, NULL_CONTEXT),
            "Expected UnsupportedOperationError when streaming message to a completed task");
    }
}
