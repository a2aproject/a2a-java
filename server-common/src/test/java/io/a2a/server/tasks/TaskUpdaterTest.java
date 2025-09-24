package io.a2a.server.tasks;

import static io.a2a.spec.Message.Role.AGENT;
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
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskUpdaterTest {
    public static final String TEST_TASK_ID = "test-task-id";
    public static final String TEST_TASK_CONTEXT_ID = "test-task-context-id";

    private static final Message SAMPLE_MESSAGE = new Message.Builder()
            .taskId(TEST_TASK_ID)
            .contextId(TEST_TASK_CONTEXT_ID)
            .parts(new TextPart("Test message"))
            .role(AGENT)
            .build();

    private static final List<Part<?>> SAMPLE_PARTS = List.of(new TextPart("Test message"));

    EventQueue eventQueue;
    private TaskUpdater taskUpdater;



    @BeforeEach
    public void init() {
        eventQueue = EventQueue.create();
        RequestContext context = new RequestContext.Builder()
                .setTaskId(TEST_TASK_ID)
                .setContextId(TEST_TASK_CONTEXT_ID)
                .build();
        taskUpdater = new TaskUpdater(context, eventQueue);
    }

    @Test
    public void testAddArtifactWithCustomIdAndName() throws Exception {
        taskUpdater.addArtifact(SAMPLE_PARTS, "custom-artifact-id", "Custom Artifact", null);
        Event event = eventQueue.dequeueEvent(0);
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals(TEST_TASK_ID, taue.getTaskId());
        assertEquals(TEST_TASK_CONTEXT_ID, taue.getContextId());
        assertEquals("custom-artifact-id", taue.getArtifact().artifactId());
        assertEquals("Custom Artifact", taue.getArtifact().name());
        assertSame(SAMPLE_PARTS, taue.getArtifact().parts());


        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testCompleteWithoutMessage() throws Exception {
        taskUpdater.complete();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.COMPLETED, null);
    }

    @Test
    public void testCompleteWithMessage() throws Exception {
        taskUpdater.complete(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.COMPLETED, SAMPLE_MESSAGE);
    }

    @Test
    public void testSubmitWithoutMessage() throws Exception {
        taskUpdater.submit();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.SUBMITTED, null);
    }

    @Test
    public void testSubmitWithMessage() throws Exception {
        taskUpdater.submit(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.SUBMITTED, SAMPLE_MESSAGE);
    }

    @Test
    public void testStartWorkWithoutMessage() throws Exception {
        taskUpdater.startWork();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.WORKING, null);
    }

    @Test
    public void testStartWorkWithMessage() throws Exception {
        taskUpdater.startWork(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.WORKING, SAMPLE_MESSAGE);
    }

    @Test
    public void testFailedWithoutMessage() throws Exception {
        taskUpdater.fail();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.FAILED, null);
    }

    @Test
    public void testFailedWithMessage() throws Exception {
        taskUpdater.fail(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.FAILED, SAMPLE_MESSAGE);
    }

    @Test
    public void testCanceledWithoutMessage() throws Exception {
        taskUpdater.cancel();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.CANCELED, null);
    }

    @Test
    public void testCanceledWithMessage() throws Exception {
        taskUpdater.cancel(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.CANCELED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRejectWithoutMessage() throws Exception {
        taskUpdater.reject();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.REJECTED, null);
    }

    @Test
    public void testRejectWithMessage() throws Exception {
        taskUpdater.reject(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.REJECTED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresInputWithoutMessage() throws Exception {
        taskUpdater.requiresInput();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.INPUT_REQUIRED, null);
    }

    @Test
    public void testRequiresInputWithMessage() throws Exception {
        taskUpdater.requiresInput(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.INPUT_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresInputWithFinalTrue() throws Exception {
        taskUpdater.requiresInput(true);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.INPUT_REQUIRED, null);
    }

    @Test
    public void testRequiresInputWithMessageAndFinalTrue() throws Exception {
        taskUpdater.requiresInput(SAMPLE_MESSAGE, true);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.INPUT_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresAuthWithoutMessage() throws Exception {
        taskUpdater.requiresAuth();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.AUTH_REQUIRED, null);
    }

    @Test
    public void testRequiresAuthWithMessage() throws Exception {
        taskUpdater.requiresAuth(SAMPLE_MESSAGE);
        checkTaskStatusUpdateEventOnQueue(false, TaskState.AUTH_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testRequiresAuthWithFinalTrue() throws Exception {
        taskUpdater.requiresAuth(true);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.AUTH_REQUIRED, null);
    }

    @Test
    public void testRequiresAuthWithMessageAndFinalTrue() throws Exception {
        taskUpdater.requiresAuth(SAMPLE_MESSAGE, true);
        checkTaskStatusUpdateEventOnQueue(true, TaskState.AUTH_REQUIRED, SAMPLE_MESSAGE);
    }

    @Test
    public void testNonTerminalStateUpdatesAllowed() throws Exception {
        // Non-terminal states should be allowed multiple times
        taskUpdater.submit();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.SUBMITTED, null);

        taskUpdater.startWork();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.WORKING, null);

        taskUpdater.requiresInput();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.INPUT_REQUIRED, null);

        taskUpdater.requiresAuth();
        checkTaskStatusUpdateEventOnQueue(false, TaskState.AUTH_REQUIRED, null);

        // Should still be able to complete
        taskUpdater.complete();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.COMPLETED, null);
    }

    @Test
    public void testNewAgentMessage() throws Exception {
        Message message = taskUpdater.newAgentMessage(SAMPLE_PARTS, null);

        assertEquals(AGENT, message.getRole());
        assertEquals(TEST_TASK_ID, message.getTaskId());
        assertEquals(TEST_TASK_CONTEXT_ID, message.getContextId());
        assertNotNull(message.getMessageId());
        assertEquals(SAMPLE_PARTS, message.getParts());
        assertNull(message.getMetadata());
    }

    @Test
    public void testNewAgentMessageWithMetadata() throws Exception {
        Map<String, Object> metadata = Map.of("key", "value");
        Message message = taskUpdater.newAgentMessage(SAMPLE_PARTS, metadata);

        assertEquals(AGENT, message.getRole());
        assertEquals(TEST_TASK_ID, message.getTaskId());
        assertEquals(TEST_TASK_CONTEXT_ID, message.getContextId());
        assertNotNull(message.getMessageId());
        assertEquals(SAMPLE_PARTS, message.getParts());
        assertEquals(metadata, message.getMetadata());
    }

    @Test
    public void testAddArtifactWithAppendTrue() throws Exception {
        taskUpdater.addArtifact(SAMPLE_PARTS, "artifact-id", "Test Artifact", null, true, null);
        Event event = eventQueue.dequeueEvent(0);
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals(TEST_TASK_ID, taue.getTaskId());
        assertEquals(TEST_TASK_CONTEXT_ID, taue.getContextId());
        assertEquals("artifact-id", taue.getArtifact().artifactId());
        assertEquals("Test Artifact", taue.getArtifact().name());
        assertSame(SAMPLE_PARTS, taue.getArtifact().parts());
        assertEquals(true, taue.isAppend());
        assertNull(taue.isLastChunk());

        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testAddArtifactWithLastChunkTrue() throws Exception {
        taskUpdater.addArtifact(SAMPLE_PARTS, "artifact-id", "Test Artifact", null, null, true);
        Event event = eventQueue.dequeueEvent(0);
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals("artifact-id", taue.getArtifact().artifactId());
        assertNull(taue.isAppend());
        assertEquals(true, taue.isLastChunk());

        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testAddArtifactWithAppendAndLastChunk() throws Exception {
        taskUpdater.addArtifact(SAMPLE_PARTS, "artifact-id", "Test Artifact", null, true, false);
        Event event = eventQueue.dequeueEvent(0);
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertEquals(true, taue.isAppend());
        assertEquals(false, taue.isLastChunk());

        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testAddArtifactGeneratesIdWhenNull() throws Exception {
        taskUpdater.addArtifact(SAMPLE_PARTS, null, "Test Artifact", null);
        Event event = eventQueue.dequeueEvent(0);
        assertNotNull(event);
        assertInstanceOf(TaskArtifactUpdateEvent.class, event);

        TaskArtifactUpdateEvent taue = (TaskArtifactUpdateEvent) event;
        assertNotNull(taue.getArtifact().artifactId());
        // Check that it's a valid UUID format
        String artifactId = taue.getArtifact().artifactId();
        assertEquals(36, artifactId.length()); // Standard UUID length
        assertTrue(artifactId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));

        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testTerminalStateProtectionAfterComplete() throws Exception {
        // Complete the task first
        taskUpdater.complete();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.COMPLETED, null);

        // Try to update status again - should throw RuntimeException
        assertThrows(RuntimeException.class, () -> taskUpdater.startWork(), 
                    "Cannot update task status - terminal state already reached");

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testTerminalStateProtectionAfterFail() throws Exception {
        // Fail the task first
        taskUpdater.fail();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.FAILED, null);

        // Try to update status again - should throw RuntimeException
        assertThrows(RuntimeException.class, () -> taskUpdater.complete(), 
                    "Cannot update task status - terminal state already reached");

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testTerminalStateProtectionAfterReject() throws Exception {
        // Reject the task first
        taskUpdater.reject();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.REJECTED, null);

        // Try to update status again - should throw RuntimeException
        assertThrows(RuntimeException.class, () -> taskUpdater.startWork(), 
                    "Cannot update task status - terminal state already reached");

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testTerminalStateProtectionAfterCancel() throws Exception {
        // Cancel the task first
        taskUpdater.cancel();
        checkTaskStatusUpdateEventOnQueue(true, TaskState.CANCELED, null);

        // Try to update status again - should throw RuntimeException
        assertThrows(RuntimeException.class, () -> taskUpdater.submit(), 
                    "Cannot update task status - terminal state already reached");

        // Verify no additional events were queued
        assertNull(eventQueue.dequeueEvent(0));
    }

    @Test
    public void testConcurrentCompletionAttempts() throws Exception {
        // This test simulates race condition between multiple completion attempts
        Thread thread1 = new Thread(() -> {
            try {
                taskUpdater.complete();
            } catch (RuntimeException e) {
                // Expected for one of the threads
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                taskUpdater.fail();
            } catch (RuntimeException e) {
                // Expected for one of the threads
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Exactly one event should have been queued
        Event event = eventQueue.dequeueEvent(0);
        assertNotNull(event);
        assertInstanceOf(TaskStatusUpdateEvent.class, event);

        TaskStatusUpdateEvent tsue = (TaskStatusUpdateEvent) event;
        assertTrue(tsue.isFinal());
        assertTrue(tsue.getStatus().state() == TaskState.COMPLETED || tsue.getStatus().state() == TaskState.FAILED);

        // No additional events should be queued
        assertNull(eventQueue.dequeueEvent(0));
    }

    private TaskStatusUpdateEvent checkTaskStatusUpdateEventOnQueue(boolean isFinal, TaskState state, Message statusMessage) throws Exception {
        Event event = eventQueue.dequeueEvent(0);

        assertNotNull(event);
        assertInstanceOf(TaskStatusUpdateEvent.class, event);

        TaskStatusUpdateEvent tsue = (TaskStatusUpdateEvent) event;
        assertEquals(TEST_TASK_ID, tsue.getTaskId());
        assertEquals(TEST_TASK_CONTEXT_ID, tsue.getContextId());
        assertEquals(isFinal, tsue.isFinal());
        assertEquals(state, tsue.getStatus().state());
        assertEquals(statusMessage, tsue.getStatus().message());

        assertNull(eventQueue.dequeueEvent(0));

        return tsue;
    }
}
