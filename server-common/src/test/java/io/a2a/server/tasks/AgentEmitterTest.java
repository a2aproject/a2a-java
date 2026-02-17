package io.a2a.server.tasks;

import static io.a2a.spec.Message.Role.ROLE_AGENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.EventQueueUtil;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AgentEmitterTest {
    public static final String TEST_TASK_ID = "test-task-id";
    public static final String TEST_TASK_CONTEXT_ID = "test-task-context-id";

    private static final Message SAMPLE_MESSAGE = Message.builder()
            .taskId(TEST_TASK_ID)
            .contextId(TEST_TASK_CONTEXT_ID)
            .parts(new TextPart("Test message"))
            .role(ROLE_AGENT)
            .build();

    private static final List<Part<?>> SAMPLE_PARTS = List.of(new TextPart("Test message"));

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};

    EventQueue eventQueue;
    private MainEventBus mainEventBus;
    private MainEventBusProcessor mainEventBusProcessor;
    private AgentEmitter agentEmitter;



    @BeforeEach
    public void init() {
        // Set up MainEventBus and processor for production-like test environment
        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        mainEventBus = new MainEventBus();
        InMemoryQueueManager queueManager = new InMemoryQueueManager(taskStore, mainEventBus);
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, taskStore, NOOP_PUSHNOTIFICATION_SENDER, queueManager);
        EventQueueUtil.start(mainEventBusProcessor);

        eventQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(TEST_TASK_ID)
                .mainEventBus(mainEventBus)
                .build().tap();
        RequestContext context = new RequestContext.Builder()
                .setTaskId(TEST_TASK_ID)
                .setContextId(TEST_TASK_CONTEXT_ID)
                .build();
        agentEmitter = new AgentEmitter(context, eventQueue);
    }

    @AfterEach
    public void cleanup() {
        if (mainEventBusProcessor != null) {
            EventQueueUtil.stop(mainEventBusProcessor);
        }
    }

    @Test
    public void testAddArtifactWithCustomIdAndName() throws Exception {
        agentEmitter.addArtifact(SAMPLE_PARTS, "custom-artifact-id", "Custom Artifact", null);
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event event = item.getEvent();
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals(TEST_TASK_ID, taue.taskId());
        assertEquals(TEST_TASK_CONTEXT_ID, taue.contextId());
        assertEquals("custom-artifact-id", taue.artifact().artifactId());
        assertEquals("Custom Artifact", taue.artifact().name());
        assertSame(SAMPLE_PARTS, taue.artifact().parts());


        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testCompleteWithoutMessage() throws Exception {
        agentEmitter.complete();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_COMPLETED, null);
    }

    @Test
    public void testCompleteWithMessage() throws Exception {
        agentEmitter.complete(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_COMPLETED, SAMPLE_MESSAGE);
    }

    @Test
    public void testSubmitWithoutMessage() throws Exception {
        agentEmitter.submit();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_SUBMITTED, null);
    }

    @Test
    public void testSubmitWithMessage() throws Exception {
        agentEmitter.submit(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_SUBMITTED, SAMPLE_MESSAGE);
    }

    @Test
    public void testStartWorkWithoutMessage() throws Exception {
        agentEmitter.startWork();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_WORKING, null);
    }

    @Test
    public void testStartWorkWithMessage() throws Exception {
        agentEmitter.startWork(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_WORKING, SAMPLE_MESSAGE);
    }

    @Test
    public void testFailedWithoutMessage() throws Exception {
        agentEmitter.fail();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_FAILED, null);
    }

    @Test
    public void testFailedWithMessage() throws Exception {
        agentEmitter.fail(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_FAILED, SAMPLE_MESSAGE);
    }

    @Test
    public void testCanceledWithoutMessage() throws Exception {
        agentEmitter.cancel();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_CANCELED, null);
    }

    @Test
    public void testCanceledWithMessage() throws Exception {
        agentEmitter.cancel(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_CANCELED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRejectWithoutMessage() throws Exception {
        agentEmitter.reject();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_REJECTED, null);
    }

    @Test
    public void testRejectWithMessage() throws Exception {
        agentEmitter.reject(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_REJECTED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresInputWithoutMessage() throws Exception {
        agentEmitter.requiresInput();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_INPUT_REQUIRED, null);
    }

    @Test
    public void testRequiresInputWithMessage() throws Exception {
        agentEmitter.requiresInput(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_INPUT_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresInputWithFinalTrue() throws Exception {
        agentEmitter.requiresInput(true);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_INPUT_REQUIRED, null);
    }

    @Test
    public void testRequiresInputWithMessageAndFinalTrue() throws Exception {
        agentEmitter.requiresInput(SAMPLE_MESSAGE, true);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_INPUT_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresAuthWithoutMessage() throws Exception {
        agentEmitter.requiresAuth();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_AUTH_REQUIRED, null);
    }

    @Test
    public void testRequiresAuthWithMessage() throws Exception {
        agentEmitter.requiresAuth(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_AUTH_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresAuthWithFinalTrue() throws Exception {
        agentEmitter.requiresAuth(true);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_AUTH_REQUIRED, null);
    }

    @Test
    public void testRequiresAuthWithMessageAndFinalTrue() throws Exception {
        agentEmitter.requiresAuth(SAMPLE_MESSAGE, true);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_AUTH_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testNonTerminalStateUpdatesAllowed() throws Exception {
        // Non-terminal states should be allowed multiple times
        agentEmitter.submit();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_SUBMITTED, null);

        agentEmitter.startWork();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_WORKING, null);

        agentEmitter.requiresInput();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_INPUT_REQUIRED, null);

        agentEmitter.requiresAuth();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.TASK_STATE_AUTH_REQUIRED, null);

        // Should still be able to complete
        agentEmitter.complete();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_COMPLETED, null);
    }

    @Test
    public void testNewAgentMessage() throws Exception {
        Message message = agentEmitter.newAgentMessage(SAMPLE_PARTS, null);

        assertEquals(ROLE_AGENT, message.role());
        assertEquals(TEST_TASK_ID, message.taskId());
        assertEquals(TEST_TASK_CONTEXT_ID, message.contextId());
        assertNotNull(message.messageId());
        assertEquals(SAMPLE_PARTS, message.parts());
        assertNull(message.metadata());
    }

    @Test
    public void testNewAgentMessageWithMetadata() throws Exception {
        Map<String, Object> metadata = Map.of("key", "value");
        Message message = agentEmitter.newAgentMessage(SAMPLE_PARTS, metadata);

        assertEquals(ROLE_AGENT, message.role());
        assertEquals(TEST_TASK_ID, message.taskId());
        assertEquals(TEST_TASK_CONTEXT_ID, message.contextId());
        assertNotNull(message.messageId());
        assertEquals(SAMPLE_PARTS, message.parts());
        assertEquals(metadata, message.metadata());
    }

    @Test
    public void testAddArtifactWithAppendTrue() throws Exception {
        agentEmitter.addArtifact(SAMPLE_PARTS, "artifact-id", "Test Artifact", null, true, null);
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event event = item.getEvent();
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals(TEST_TASK_ID, taue.taskId());
        assertEquals(TEST_TASK_CONTEXT_ID, taue.contextId());
        assertEquals("artifact-id", taue.artifact().artifactId());
        assertEquals("Test Artifact", taue.artifact().name());
        assertSame(SAMPLE_PARTS, taue.artifact().parts());
        assertEquals(true, taue.append());
        assertNull(taue.lastChunk());

        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testAddArtifactWithLastChunkTrue() throws Exception {
        agentEmitter.addArtifact(SAMPLE_PARTS, "artifact-id", "Test Artifact", null, null, true);
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event event = item.getEvent();
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals("artifact-id", taue.artifact().artifactId());
        assertNull(taue.append());
        assertEquals(true, taue.lastChunk());

        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testAddArtifactWithAppendAndLastChunk() throws Exception {
        agentEmitter.addArtifact(SAMPLE_PARTS, "artifact-id", "Test Artifact", null, true, false);
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event event = item.getEvent();
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals(true, taue.append());
        assertEquals(false, taue.lastChunk());

        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testAddArtifactGeneratesIdWhenNull() throws Exception {
        agentEmitter.addArtifact(SAMPLE_PARTS, null, "Test Artifact", null);
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event event = item.getEvent();
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertNotNull(taue.artifact().artifactId());
        // Check that it's a valid UUID format
        String artifactId = taue.artifact().artifactId();
        assertEquals(36, artifactId.length()); // Standard UUID length
        assertTrue(artifactId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));

        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testTerminalStateProtectionAfterComplete() throws Exception {
        // Complete the task first
        agentEmitter.complete();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_COMPLETED, null);

        // Try to update status again - should throw RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> agentEmitter.startWork());
        assertEquals("Cannot update task status - terminal state already reached", exception.getMessage());

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testTerminalStateProtectionAfterFail() throws Exception {
        // Fail the task first
        agentEmitter.fail();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_FAILED, null);

        // Try to update status again - should throw RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> agentEmitter.complete());
        assertEquals("Cannot update task status - terminal state already reached", exception.getMessage());

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testTerminalStateProtectionAfterReject() throws Exception {
        // Reject the task first
        agentEmitter.reject();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_REJECTED, null);

        // Try to update status again - should throw RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> agentEmitter.startWork());
        assertEquals("Cannot update task status - terminal state already reached", exception.getMessage());

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testTerminalStateProtectionAfterCancel() throws Exception {
        // Cancel the task first
        agentEmitter.cancel();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.TASK_STATE_CANCELED, null);

        // Try to update status again - should throw RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> agentEmitter.submit());
        assertEquals("Cannot update task status - terminal state already reached", exception.getMessage());

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEventItem(0));
    }

    @Test
    public void testConcurrentCompletionAttempts() throws Exception {
        // This test simulates race condition between multiple completion attempts
        Thread thread1 = new Thread(() -> {
            try {
                agentEmitter.complete();
            } catch (RuntimeException e) {
                // Expected for one of the threads
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                agentEmitter.fail();
            } catch (RuntimeException e) {
                // Expected for one of the threads
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Exactly one event should have been queued
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event event = item.getEvent();
        assertNotNull(event);
        assertInstanceOf(TaskStatusUpdateEvent.class, event);

        TaskStatusUpdateEvent tsue = (TaskStatusUpdateEvent) event;
        assertTrue(tsue.isFinal());
        assertTrue(tsue.status().state() == TaskState.TASK_STATE_COMPLETED || tsue.status().state() == TaskState.TASK_STATE_FAILED);

        // No additional events should be queued
        assertNull(eventQueue.dequeueEventItem(0));
    }

    private TaskStatusUpdateEvent checkTaskStatusUpdateEventOnQueue(boolean isFinal, TaskState state, Message statusMessage) throws Exception {
        // Wait up to 5 seconds for event (async MainEventBusProcessor needs time to distribute)
        EventQueueItem item = eventQueue.dequeueEventItem(5000);
        assertNotNull(item);
        Event event = item.getEvent();

        assertNotNull(event);
        assertInstanceOf(TaskStatusUpdateEvent.class, event);

        TaskStatusUpdateEvent tsue = (TaskStatusUpdateEvent) event;
        assertEquals(TEST_TASK_ID, tsue.taskId());
        assertEquals(TEST_TASK_CONTEXT_ID, tsue.contextId());
        assertEquals(isFinal, tsue.isFinal());
        assertEquals(state, tsue.status().state());
        assertEquals(statusMessage, tsue.status().message());

        // Check no additional events (still use 0 timeout for this check)
        assertNull(eventQueue.dequeueEventItem(0));

        return tsue;
    }
}
