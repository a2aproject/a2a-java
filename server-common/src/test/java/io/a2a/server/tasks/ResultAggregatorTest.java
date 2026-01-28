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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.a2a.server.events.EventConsumer;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueUtil;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.spec.Event;
import io.a2a.spec.EventKind;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
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
        aggregator = new ResultAggregator(mockTaskManager, null, testExecutor, testExecutor);
    }

    // Helper methods for creating sample data
    private Message createSampleMessage(String content, String msgId, Message.Role role) {
        return Message.builder()
                .messageId(msgId)
                .role(role)
                .parts(Collections.singletonList(new TextPart(content)))
                .build();
    }

    private Task createSampleTask(String taskId, TaskState statusState, String contextId) {
        return Task.builder()
                .id(taskId)
                .contextId(contextId)
                .status(new TaskStatus(statusState))
                .build();
    }

    /**
     * Helper to wait for MainEventBusProcessor to process an event.
     * Replaces polling patterns with deterministic callback-based waiting.
     *
     * @param processor the processor to set callback on
     * @param action the action that triggers event processing
     * @throws InterruptedException if waiting is interrupted
     * @throws AssertionError if processing doesn't complete within timeout
     */
    private void waitForEventProcessing(MainEventBusProcessor processor, Runnable action) throws InterruptedException {
        CountDownLatch processingLatch = new CountDownLatch(1);
        processor.setCallback(new io.a2a.server.events.MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event event) {
                processingLatch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
                // Not needed for basic event processing wait
            }
        });

        try {
            action.run();
            assertTrue(processingLatch.await(5, TimeUnit.SECONDS),
                    "MainEventBusProcessor should have processed the event within timeout");
        } finally {
            processor.setCallback(null);
        }
    }


    // Basic functionality tests

    @Test
    void testConstructorWithMessage() {
        Message initialMessage = createSampleMessage("initial", "msg1", Message.Role.USER);
        ResultAggregator aggregatorWithMessage = new ResultAggregator(mockTaskManager, initialMessage, testExecutor, testExecutor);

        // Test that the message is properly stored by checking getCurrentResult
        assertEquals(initialMessage, aggregatorWithMessage.getCurrentResult());
        // TaskManager should not be called when message is set
        verifyNoInteractions(mockTaskManager);
    }

    @Test
    void testGetCurrentResultWithMessageSet() {
        Message sampleMessage = createSampleMessage("hola", "msg1", Message.Role.USER);
        ResultAggregator aggregatorWithMessage = new ResultAggregator(mockTaskManager, sampleMessage, testExecutor, testExecutor);

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
        ResultAggregator aggregatorWithNullMessage = new ResultAggregator(mockTaskManager, null, testExecutor, testExecutor);
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
        ResultAggregator messageAggregator = new ResultAggregator(mockTaskManager, message, testExecutor, testExecutor);

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
        String taskId = "test-task";
        Task firstEvent = createSampleTask(taskId, TaskState.WORKING, "ctx1");

        // After processing firstEvent, the current result will be that task
        when(mockTaskManager.getTask()).thenReturn(firstEvent);

        // Create an event queue using QueueManager (which has access to builder)
        MainEventBus mainEventBus = new MainEventBus();
        InMemoryTaskStore taskStore = new InMemoryTaskStore();
        InMemoryQueueManager queueManager =
            new InMemoryQueueManager(new MockTaskStateProvider(), mainEventBus);
        MainEventBusProcessor processor = new MainEventBusProcessor(mainEventBus, taskStore, task -> {}, queueManager);
        EventQueueUtil.start(processor);

        EventQueue queue = queueManager.getEventQueueBuilder(taskId).build().tap();

        // Use callback to wait for event processing (replaces polling)
        waitForEventProcessing(processor, () -> queue.enqueueEvent(firstEvent));

        // Create real EventConsumer with the queue
        EventConsumer eventConsumer =
            new EventConsumer(queue);

        // Close queue after first event to simulate stream ending after processing
        queue.close();

        ResultAggregator.EventTypeAndInterrupt result =
            aggregator.consumeAndBreakOnInterrupt(eventConsumer, false);

        assertEquals(firstEvent, result.eventType());
        assertTrue(result.interrupted());
        // NOTE: ResultAggregator no longer calls taskManager.process()
        // That responsibility has moved to MainEventBusProcessor for centralized persistence
        //
        // NOTE: Since firstEvent is a Task, ResultAggregator captures it directly from the queue
        // (capturedTask.get() at line 283 in ResultAggregator). Therefore, taskManager.getTask()
        // is only called for debug logging in taskIdForLogging() (line 305), which may or may not
        // execute depending on timing and log level. We expect 0-1 calls, not 1-2.
        verify(mockTaskManager, atMost(1)).getTask();

        // Cleanup: stop the processor
        EventQueueUtil.stop(processor);
    }
}
