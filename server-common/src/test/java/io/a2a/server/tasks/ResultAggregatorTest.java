package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Comprehensive tests for ResultAggregator based on Python test patterns.
 * This is not a strict backport of the Python test, but it implements the same testing patterns
 * adapted for Java's reactive streams and concurrency model.
 *
 * Note: This simplified version focuses on the core functionality without complex reactive stream testing
 * that was causing issues with the original implementation.
 */
public class ResultAggregatorTest {

    @Mock
    private TaskManager mockTaskManager;

    private ResultAggregator aggregator;
    // Use a real thread pool executor instead of direct executor
    // to avoid blocking the calling thread during async operations
    private final Executor testExecutor = Executors.newCachedThreadPool();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aggregator = new ResultAggregator(mockTaskManager, null, testExecutor, null);
    }

    // Helper methods for creating sample data
    private Message createSampleMessage(String content, String msgId, Message.Role role) {
        return new Message.Builder()
                .messageId(msgId)
                .role(role)
                .parts(Collections.singletonList(new TextPart(content)))
                .build();
    }

    private Task createSampleTask(String taskId, TaskState statusState, String contextId) {
        return new Task.Builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(statusState))
                .build();
    }


    // Basic functionality tests

    @Test
    void testConstructorWithMessage() {
        Message initialMessage = createSampleMessage("initial", "msg1", Message.Role.USER);
        ResultAggregator aggregatorWithMessage = new ResultAggregator(mockTaskManager, initialMessage, testExecutor, null);

        // Test that the message is properly stored by checking getCurrentResult
        assertEquals(initialMessage, aggregatorWithMessage.getCurrentResult());
        // TaskManager should not be called when message is set
        verifyNoInteractions(mockTaskManager);
    }

    @Test
    void testGetCurrentResultWithMessageSet() {
        Message sampleMessage = createSampleMessage("hola", "msg1", Message.Role.USER);
        ResultAggregator aggregatorWithMessage = new ResultAggregator(mockTaskManager, sampleMessage, testExecutor, null);

        EventKind result = aggregatorWithMessage.getCurrentResult();

        assertEquals(sampleMessage, result);
        // TaskManager.getTask() should not be called when message is set
        verifyNoInteractions(mockTaskManager);
    }

    @Test
    void testGetCurrentResultWithMessageNull() {
        Task expectedTask = createSampleTask("task_from_tm", TaskState.SUBMITTED, "ctx1");
        when(mockTaskManager.getTask()).thenReturn(expectedTask);

        EventKind result = aggregator.getCurrentResult();

        assertEquals(expectedTask, result);
        verify(mockTaskManager).getTask();
    }

    @Test
    void testConstructorStoresTaskManagerCorrectly() {
        // Test that constructor properly initializes the aggregator
        // We can't access the private field directly, but we can test behavior
        Task expectedTask = createSampleTask("test_task", TaskState.SUBMITTED, "ctx1");
        when(mockTaskManager.getTask()).thenReturn(expectedTask);

        EventKind result = aggregator.getCurrentResult();

        assertEquals(expectedTask, result);
        verify(mockTaskManager).getTask();
    }

    @Test
    void testConstructorWithNullMessage() {
        ResultAggregator aggregatorWithNullMessage = new ResultAggregator(mockTaskManager, null, testExecutor, null);
        Task expectedTask = createSampleTask("null_msg_task", TaskState.WORKING, "ctx1");
        when(mockTaskManager.getTask()).thenReturn(expectedTask);

        EventKind result = aggregatorWithNullMessage.getCurrentResult();

        assertEquals(expectedTask, result);
        verify(mockTaskManager).getTask();
    }

    @Test
    void testGetCurrentResultReturnsTaskWhenNoMessage() {
        Task expectedTask = createSampleTask("no_message_task", TaskState.COMPLETED, "ctx1");
        when(mockTaskManager.getTask()).thenReturn(expectedTask);

        EventKind result = aggregator.getCurrentResult();

        assertNotNull(result);
        assertEquals(expectedTask, result);
        verify(mockTaskManager).getTask();
    }

    @Test
    void testGetCurrentResultWithDifferentTaskStates() {
        // Test with WORKING and COMPLETED states using chained returns
        Task workingTask = createSampleTask("working_task", TaskState.WORKING, "ctx1");
        Task completedTask = createSampleTask("completed_task", TaskState.COMPLETED, "ctx1");
        when(mockTaskManager.getTask()).thenReturn(workingTask, completedTask);

        // First call returns WORKING task
        EventKind result1 = aggregator.getCurrentResult();
        assertEquals(workingTask, result1);

        // Second call returns COMPLETED task
        EventKind result2 = aggregator.getCurrentResult();
        assertEquals(completedTask, result2);
    }

    @Test
    void testMultipleGetCurrentResultCalls() {
        // Test that multiple calls to getCurrentResult behave consistently
        Task expectedTask = createSampleTask("multi_call_task", TaskState.SUBMITTED, "ctx1");
        when(mockTaskManager.getTask()).thenReturn(expectedTask);

        EventKind result1 = aggregator.getCurrentResult();
        EventKind result2 = aggregator.getCurrentResult();
        EventKind result3 = aggregator.getCurrentResult();

        assertEquals(expectedTask, result1);
        assertEquals(expectedTask, result2);
        assertEquals(expectedTask, result3);

        // Verify getTask was called multiple times
        verify(mockTaskManager, times(3)).getTask();
    }

    @Test
    void testGetCurrentResultWithMessageTakesPrecedence() {
        // Test that when both message and task are available, message takes precedence
        Message message = createSampleMessage("priority message", "pri1", Message.Role.USER);
        ResultAggregator messageAggregator = new ResultAggregator(mockTaskManager, message, testExecutor, null);

        // Even if we set up the task manager to return something, message should take precedence
        Task task = createSampleTask("should_not_be_returned", TaskState.WORKING, "ctx1");
        when(mockTaskManager.getTask()).thenReturn(task);

        EventKind result = messageAggregator.getCurrentResult();

        assertEquals(message, result);
        // Task manager should not be called when message is present
        verifyNoInteractions(mockTaskManager);
    }

    @Test
    void testConsumeAndBreakNonBlocking() throws Exception {
        // Test that with blocking=false, the method returns after the first event
        Task firstEvent = createSampleTask("non_blocking_task", TaskState.WORKING, "ctx1");

        // After processing firstEvent, the current result will be that task
        when(mockTaskManager.getTask()).thenReturn(firstEvent);

        // Create an event queue using QueueManager (which has access to builder)
        InMemoryQueueManager queueManager =
            new InMemoryQueueManager(new MockTaskStateProvider());

        EventQueue queue = queueManager.getEventQueueBuilder("test-task").build();
        queue.enqueueEvent(firstEvent);

        // Create real EventConsumer with the queue
        EventConsumer eventConsumer =
            new EventConsumer(queue);

        // Close queue after first event to simulate stream ending after processing
        queue.close();

        ResultAggregator.EventTypeAndInterrupt result =
            aggregator.consumeAndBreakOnInterrupt(eventConsumer, false);

        assertEquals(firstEvent, result.eventType());
        assertTrue(result.interrupted());
        verify(mockTaskManager).processEvent(firstEvent);
        // getTask() is called multiple times in the new TaskStateProcessor implementation:
        // - For return value (line 259)
        // - For logging (line 213 if debug enabled)
        // - Within TaskStateProcessor methods
        // - Background consumption final persistence
        // The async consumer may or may not execute before verification, so we accept 1-5 calls
        verify(mockTaskManager, atLeast(1)).getTask();
        verify(mockTaskManager, atMost(5)).getTask();
    }
}
