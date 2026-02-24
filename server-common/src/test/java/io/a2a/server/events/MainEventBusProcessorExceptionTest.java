package io.a2a.server.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskPersistenceException;
import io.a2a.server.tasks.TaskSerializationException;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Event;
import io.a2a.spec.InternalError;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Integration tests for MainEventBusProcessor exception handling.
 * <p>
 * Tests verify that TaskStore persistence failures are converted to InternalError events
 * and distributed to clients with appropriate logging based on failure type:
 * <ul>
 *   <li>TaskSerializationException → ERROR log + InternalError</li>
 *   <li>TaskPersistenceException (transient) → WARN log + InternalError</li>
 *   <li>TaskPersistenceException (permanent) → ERROR log + InternalError</li>
 * </ul>
 */
public class MainEventBusProcessorExceptionTest {

    private static final PushNotificationSender NOOP_PUSHNOTIFICATION_SENDER = task -> {};
    private static final String TASK_ID = "test-task-123";

    private MainEventBus mainEventBus;
    private MainEventBusProcessor mainEventBusProcessor;
    private TaskStore mockTaskStore;
    private InMemoryQueueManager queueManager;
    private EventQueue eventQueue;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    public void setUp() {
        // Set up mock TaskStore
        mockTaskStore = mock(TaskStore.class);

        // Set up MainEventBus and processor with mock TaskStore
        mainEventBus = new MainEventBus();
        queueManager = new InMemoryQueueManager(null, mainEventBus); // null TaskStateProvider for tests
        mainEventBusProcessor = new MainEventBusProcessor(mainEventBus, mockTaskStore,
                                                          NOOP_PUSHNOTIFICATION_SENDER, queueManager);

        // Set up log capture for verifying error messages
        Logger logger = (Logger) LoggerFactory.getLogger(MainEventBusProcessor.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        // Start processor
        EventQueueUtil.start(mainEventBusProcessor);

        // Create event queue for testing
        eventQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(TASK_ID)
                .mainEventBus(mainEventBus)
                .build().tap();
    }

    @AfterEach
    public void tearDown() {
        if (mainEventBusProcessor != null) {
            mainEventBusProcessor.setCallback(null);
            EventQueueUtil.stop(mainEventBusProcessor);
        }

        // Clean up log appender
        Logger logger = (Logger) LoggerFactory.getLogger(MainEventBusProcessor.class);
        logger.detachAppender(logAppender);
    }

    /**
     * Test that TaskSerializationException is converted to InternalError with ERROR log.
     * AC#1: Mock TaskStore throws TaskSerializationException → MainEventBusProcessor distributes InternalError
     */
    @Test
    public void testTaskSerializationException_ConvertsToInternalError() throws InterruptedException {
        // Arrange: Mock TaskStore to throw TaskSerializationException
        String exceptionMessage = "Failed to deserialize corrupted JSON";
        TaskSerializationException exception = new TaskSerializationException(TASK_ID, exceptionMessage);
        when(mockTaskStore.get(any())).thenThrow(exception);
        doThrow(exception).when(mockTaskStore).save(any(Task.class), anyBoolean());

        Task testTask = createTestTask();

        // Act: Enqueue event and wait for processing
        List<Event> distributedEvents = captureDistributedEvent(testTask);

        // Assert: Verify InternalError was distributed
        assertEquals(1, distributedEvents.size(), "Should distribute exactly one event");
        Event distributedEvent = distributedEvents.get(0);
        assertInstanceOf(InternalError.class, distributedEvent,
                        "TaskSerializationException should convert to InternalError");

        InternalError error = (InternalError) distributedEvent;
        assertTrue(error.getMessage().contains(TASK_ID),
                  "Error message should contain task ID: " + error.getMessage());
        assertTrue(error.getMessage().contains("serialize"),
                  "Error message should mention serialization: " + error.getMessage());

        // Assert: Verify ERROR level logging
        boolean foundErrorLog = logAppender.list.stream()
            .anyMatch(event -> event.getLevel() == Level.ERROR
                            && event.getFormattedMessage().contains(TASK_ID)
                            && event.getFormattedMessage().contains("serialization"));
        assertTrue(foundErrorLog, "Should log TaskSerializationException at ERROR level");
    }

    /**
     * Test that TaskPersistenceException (transient) is converted to InternalError with WARN log.
     * AC#2: Mock TaskStore throws TaskPersistenceException(transient) → WARN log + InternalError
     */
    @Test
    public void testTaskPersistenceException_Transient_ConvertsToInternalError() throws InterruptedException {
        // Arrange: Mock TaskStore to throw transient TaskPersistenceException
        String exceptionMessage = "Connection timeout to database";
        TaskPersistenceException exception = new TaskPersistenceException(
            TASK_ID, exceptionMessage, true // transient = true
        );
        when(mockTaskStore.get(any())).thenThrow(exception);
        doThrow(exception).when(mockTaskStore).save(any(Task.class), anyBoolean());

        Task testTask = createTestTask();

        // Act: Enqueue event and wait for processing
        List<Event> distributedEvents = captureDistributedEvent(testTask);

        // Assert: Verify InternalError was distributed
        assertEquals(1, distributedEvents.size(), "Should distribute exactly one event");
        Event distributedEvent = distributedEvents.get(0);
        assertInstanceOf(InternalError.class, distributedEvent,
                        "TaskPersistenceException should convert to InternalError");

        InternalError error = (InternalError) distributedEvent;
        assertTrue(error.getMessage().contains(TASK_ID),
                  "Error message should contain task ID: " + error.getMessage());
        assertTrue(error.getMessage().contains("retry may succeed"),
                  "Transient error message should mention retry: " + error.getMessage());

        // Assert: Verify WARN level logging for transient failures
        boolean foundWarnLog = logAppender.list.stream()
            .anyMatch(event -> event.getLevel() == Level.WARN
                            && event.getFormattedMessage().contains(TASK_ID)
                            && event.getFormattedMessage().contains("TRANSIENT"));
        assertTrue(foundWarnLog, "Should log transient TaskPersistenceException at WARN level");
    }

    /**
     * Test that TaskPersistenceException (permanent) is converted to InternalError with ERROR log.
     * AC#3: Mock TaskStore throws TaskPersistenceException(permanent) → ERROR log + InternalError
     */
    @Test
    public void testTaskPersistenceException_Permanent_ConvertsToInternalError() throws InterruptedException {
        // Arrange: Mock TaskStore to throw permanent TaskPersistenceException
        String exceptionMessage = "Disk full - cannot write";
        TaskPersistenceException exception = new TaskPersistenceException(
            TASK_ID, exceptionMessage, false // transient = false (permanent)
        );
        when(mockTaskStore.get(any())).thenThrow(exception);
        doThrow(exception).when(mockTaskStore).save(any(Task.class), anyBoolean());

        Task testTask = createTestTask();

        // Act: Enqueue event and wait for processing
        List<Event> distributedEvents = captureDistributedEvent(testTask);

        // Assert: Verify InternalError was distributed
        assertEquals(1, distributedEvents.size(), "Should distribute exactly one event");
        Event distributedEvent = distributedEvents.get(0);
        assertInstanceOf(InternalError.class, distributedEvent,
                        "TaskPersistenceException should convert to InternalError");

        InternalError error = (InternalError) distributedEvent;
        assertTrue(error.getMessage().contains(TASK_ID),
                  "Error message should contain task ID: " + error.getMessage());
        assertTrue(error.getMessage().contains("requires intervention"),
                  "Permanent error message should mention intervention: " + error.getMessage());

        // Assert: Verify ERROR level logging for permanent failures
        boolean foundErrorLog = logAppender.list.stream()
            .anyMatch(event -> event.getLevel() == Level.ERROR
                            && event.getFormattedMessage().contains(TASK_ID)
                            && event.getFormattedMessage().contains("PERMANENT"));
        assertTrue(foundErrorLog, "Should log permanent TaskPersistenceException at ERROR level");
    }

    /**
     * Test that taskId is preserved through exception chain and appears in error messages.
     * AC#5: All tests validate error messages contain taskId and failure type
     */
    @Test
    public void testTaskIdPreservedInExceptionChain() throws InterruptedException {
        // Arrange: Create exception with specific taskId
        String specificTaskId = "task-with-unique-id-12345";
        TaskSerializationException exception = new TaskSerializationException(
            specificTaskId, "Test exception with specific task ID"
        );
        when(mockTaskStore.get(any())).thenThrow(exception);
        doThrow(exception).when(mockTaskStore).save(any(Task.class), anyBoolean());

        // Create event queue with specific taskId
        EventQueue specificQueue = EventQueueUtil.getEventQueueBuilder(mainEventBus)
                .taskId(specificTaskId)
                .mainEventBus(mainEventBus)
                .build().tap();

        Task testTask = Task.builder()
                .id(specificTaskId)
                .contextId("test-context")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();

        // Act: Enqueue event and wait for processing
        List<Event> distributedEvents = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        mainEventBusProcessor.setCallback(new MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event event) {
                distributedEvents.add(event);
                latch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
                // No-op for this test
            }
        });

        specificQueue.enqueueEvent(testTask);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Event processing should complete within timeout");

        // Assert: Verify specific taskId appears in distributed error
        assertEquals(1, distributedEvents.size());
        InternalError error = (InternalError) distributedEvents.get(0);
        assertTrue(error.getMessage().contains(specificTaskId),
                  "Error should contain specific task ID: " + error.getMessage());

        // Assert: Verify specific taskId appears in logs
        boolean foundTaskIdInLog = logAppender.list.stream()
            .anyMatch(event -> event.getFormattedMessage().contains(specificTaskId));
        assertTrue(foundTaskIdInLog, "Logs should contain specific task ID");
    }

    /**
     * Helper method to create a test Task.
     */
    private Task createTestTask() {
        return Task.builder()
                .id(TASK_ID)
                .contextId("test-context")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
    }

    /**
     * Helper method to enqueue an event and capture what gets distributed to clients.
     * Uses MainEventBusProcessorCallback to wait for async processing.
     *
     * @param event the event to enqueue
     * @return list of events distributed to ChildQueues (should be 1 event)
     */
    private List<Event> captureDistributedEvent(Event event) throws InterruptedException {
        List<Event> distributedEvents = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mainEventBusProcessor.setCallback(new MainEventBusProcessorCallback() {
            @Override
            public void onEventProcessed(String taskId, Event processedEvent) {
                distributedEvents.add(processedEvent);
                latch.countDown();
            }

            @Override
            public void onTaskFinalized(String taskId) {
                // No-op for exception tests
            }
        });

        eventQueue.enqueueEvent(event);

        assertTrue(latch.await(5, TimeUnit.SECONDS),
                  "Event processing should complete within timeout");

        return distributedEvents;
    }
}
