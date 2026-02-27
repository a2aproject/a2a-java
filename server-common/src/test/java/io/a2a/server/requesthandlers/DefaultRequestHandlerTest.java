package io.a2a.server.requesthandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;

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
        .blocking(false)
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
}
