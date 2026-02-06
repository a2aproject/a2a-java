package io.a2a.extras.queuemanager.replicated.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.extras.queuemanager.replicated.core.ReplicatedEventQueueItem;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.QueueClosedEvent;
import io.a2a.server.events.QueueManager;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for Kafka replication functionality.
 * Tests the full A2A message flow with Kafka replication verification.
 */
@QuarkusTest
public class KafkaReplicationIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaReplicationIntegrationTest.class);

    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    @Inject
    TestKafkaEventConsumer testConsumer;

    @Inject
    @Channel("replicated-events-out")
    Emitter<String> testEmitter;

    @Inject
    QueueManager queueManager;

    private Client streamingClient;
    private Client nonStreamingClient;
    private Client pollingClient;

    @BeforeEach
    public void setup() throws A2AClientException {
        // Create non-streaming client for initial task creation
        ClientConfig nonStreamingConfig = new ClientConfig.Builder()
                .setStreaming(false)
                .build();

        nonStreamingClient = Client.builder(agentCard)
                .clientConfig(nonStreamingConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();

        // Create streaming client for resubscription
        ClientConfig streamingConfig = new ClientConfig.Builder()
                .setStreaming(true)
                .build();

        streamingClient = Client.builder(agentCard)
                .clientConfig(streamingConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();

        // Create polling client for non-blocking operations
        ClientConfig pollingConfig = new ClientConfig.Builder()
                .setStreaming(false)
                .setPolling(true)
                .build();

        pollingClient = Client.builder(agentCard)
                .clientConfig(pollingConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Close all clients to release resources and prevent connection leaks
        if (nonStreamingClient != null) {
            nonStreamingClient.close();
        }
        if (streamingClient != null) {
            streamingClient.close();
        }
        if (pollingClient != null) {
            pollingClient.close();
        }
    }

    @Test
    public void testA2AMessageReplicatedToKafka() throws Exception {
        String taskId = "kafka-replication-test-" + System.currentTimeMillis();
        String contextId = "test-context-" + System.currentTimeMillis();

        // Clear any previous events
        testConsumer.clear();

        // Send A2A message that should trigger events and replication
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("create")))
                .taskId(taskId)
                .messageId("test-msg-" + System.currentTimeMillis())
                .contextId(contextId)
                .build();

        CountDownLatch a2aLatch = new CountDownLatch(1);
        AtomicReference<Task> createdTask = new AtomicReference<>();
        AtomicBoolean a2aError = new AtomicBoolean(false);

        // Send message and verify A2A processing works
        nonStreamingClient.sendMessage(message, List.of((ClientEvent event, AgentCard card) -> {
            if (event instanceof TaskEvent taskEvent) {
                createdTask.set(taskEvent.getTask());
                a2aLatch.countDown();
            }
        }), (Throwable error) -> {
            a2aError.set(true);
            a2aLatch.countDown();
        });

        // Wait for A2A processing to complete
        assertTrue(a2aLatch.await(15, TimeUnit.SECONDS), "A2A message processing timed out");
        assertFalse(a2aError.get(), "A2A processing failed");

        Task task = createdTask.get();
        assertNotNull(task, "Task should be created");
        assertEquals(taskId, task.id());
        assertEquals(TaskState.SUBMITTED, task.status().state());

        // Wait for the event to be replicated to Kafka
        ReplicatedEventQueueItem replicatedEvent = testConsumer.waitForEvent(taskId, 30);
        assertNotNull(replicatedEvent, "Event should be replicated to Kafka within 30 seconds");

        // Verify the replicated event content
        assertEquals(taskId, replicatedEvent.getTaskId());
        io.a2a.spec.Event receivedEvent = replicatedEvent.getEvent();
        assertNotNull(receivedEvent);

        // The event should now maintain its proper type through Kafka serialization
        // This verifies that our polymorphic serialization is working correctly
        assertInstanceOf(TaskStatusUpdateEvent.class, receivedEvent, "Event should maintain TaskStatusUpdateEvent type after Kafka round-trip");
        TaskStatusUpdateEvent statusUpdateEvent = (TaskStatusUpdateEvent) receivedEvent;

        // Verify the event data is consistent with the task returned from the client
        assertEquals(taskId, statusUpdateEvent.taskId(), "Event task ID should match original task ID");
        assertEquals(contextId, statusUpdateEvent.contextId(), "Event context ID should match original context ID");
        assertEquals(TaskState.SUBMITTED, statusUpdateEvent.status().state(), "Event should show SUBMITTED state");
        assertFalse(statusUpdateEvent.isFinal(), "Event should show final:false");
        assertEquals(TaskStatusUpdateEvent.STREAMING_EVENT_ID, statusUpdateEvent.kind(), "Event should indicate status-update type");
    }

    @Test
    public void testKafkaEventReceivedByA2AServer() throws Exception {
        String taskId = "kafka-to-a2a-test-" + System.currentTimeMillis();
        String contextId = "test-context-" + System.currentTimeMillis();

        // Clear any previous events
        testConsumer.clear();

        // First create a task in the A2A system using non-streaming client
        Message createMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("create")))
                .taskId(taskId)
                .messageId("create-msg-" + System.currentTimeMillis())
                .contextId(contextId)
                .build();

        CountDownLatch createLatch = new CountDownLatch(1);
        AtomicReference<Task> createdTask = new AtomicReference<>();

        nonStreamingClient.sendMessage(createMessage, List.of((ClientEvent event, AgentCard card) -> {
            if (event instanceof TaskEvent taskEvent) {
                createdTask.set(taskEvent.getTask());
                createLatch.countDown();
            }
        }), (Throwable error) -> {
            createLatch.countDown();
        });

        assertTrue(createLatch.await(15, TimeUnit.SECONDS), "Task creation timed out");
        Task initialTask = createdTask.get();
        assertNotNull(initialTask, "Task should be created");
        assertEquals(TaskState.SUBMITTED, initialTask.status().state(), "Initial task should be in SUBMITTED state");

        // Add a small delay to ensure the task is fully processed before resubscription
        Thread.sleep(1000);

        // Set up resubscription to listen for task updates using streaming client
        CountDownLatch subscribeLatch = new CountDownLatch(1);
        AtomicReference<TaskStatusUpdateEvent> receivedCompletedEvent = new AtomicReference<>();
        AtomicBoolean wasUnexpectedEvent = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicBoolean receivedInitialTask = new AtomicBoolean(false);

        // Create consumer to handle subscribed events
        BiConsumer<ClientEvent, AgentCard> consumer = (event, agentCard) -> {
            // Per A2A spec 3.1.6: ENFORCE that first event is TaskEvent
            if (!receivedInitialTask.get()) {
                if (event instanceof TaskEvent) {
                    receivedInitialTask.set(true);
                    return;
                } else {
                    throw new AssertionError("First event on subscribe MUST be TaskEvent, but was: " + event.getClass().getSimpleName());
                }
            }

            // Process subsequent events
            if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                if (taskUpdateEvent.getUpdateEvent() instanceof TaskStatusUpdateEvent statusEvent) {
                    if (statusEvent.status().state() == TaskState.COMPLETED) {
                        receivedCompletedEvent.set(statusEvent);
                        subscribeLatch.countDown();
                    }
                } else {
                    wasUnexpectedEvent.set(true);
                }
            } else {
                wasUnexpectedEvent.set(true);
            }
        };

        // Create error handler - filter out benign stream closed errors.
        // HTTP/2 streams are cancelled during normal cleanup when subscriptions end,
        // which is expected behavior and not an actual error condition.
        Consumer<Throwable> errorHandler = error -> {
            if (!isStreamClosedError(error)) {
                errorRef.set(error);
            }
            subscribeLatch.countDown();
        };

        // Subscribe to the task to listen for updates
        streamingClient.subscribeToTask(new TaskIdParams(taskId), List.of(consumer), errorHandler);

        // Now manually send a TaskStatusUpdateEvent to Kafka using reactive messaging
        TaskStatusUpdateEvent statusEvent = TaskStatusUpdateEvent.builder()
                .taskId(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();

        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, statusEvent);
        String eventJson = JsonUtil.toJson(replicatedEvent);

        // Send to Kafka using reactive messaging
        testEmitter.send(eventJson);

        // Wait for the replicated event to be received via streaming resubscription
        // This tests the full round-trip: Manual Kafka Event -> A2A System -> Streaming Client
        assertTrue(subscribeLatch.await(15, TimeUnit.SECONDS), "Should receive COMPLETED event via resubscription");

        // Verify no unexpected events or errors
        assertFalse(wasUnexpectedEvent.get(), "Should not receive unexpected events");
        assertNull(errorRef.get(), "Should not receive errors during resubscription");

        // Verify the received event
        TaskStatusUpdateEvent completedEvent = receivedCompletedEvent.get();
        assertNotNull(completedEvent, "Should have received a TaskStatusUpdateEvent");
        assertEquals(TaskState.COMPLETED, completedEvent.status().state(), "Event should show COMPLETED state");
        assertTrue(completedEvent.isFinal(), "Event should be marked as final");
        assertEquals(taskId, completedEvent.taskId(), "Event should have correct task ID");
        assertEquals(contextId, completedEvent.contextId(), "Event should have correct context ID");

        // Note: We do NOT verify TaskStore state here because replicated events intentionally
        // skip TaskStore updates to avoid duplicates. The TaskStore is updated on the originating
        // node that produced the event. This test verifies the replication flow:
        // Kafka -> EventQueue -> Streaming Clients, which is working correctly.
    }

    @Test
    public void testQueueClosedEventTerminatesRemoteSubscribers() throws Exception {
        String taskId = "queue-closed-test-" + System.currentTimeMillis();
        String contextId = "test-context-" + System.currentTimeMillis();

        // Clear any previous events
        testConsumer.clear();

        // Use polling (non-blocking) client with "working" command
        // This creates task in WORKING state (non-final) and keeps queue alive
        Message workingMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("working")))
                .taskId(taskId)
                .messageId("working-msg-" + System.currentTimeMillis())
                .contextId(contextId)
                .build();

        CountDownLatch workingLatch = new CountDownLatch(1);
        AtomicReference<String> taskIdRef = new AtomicReference<>();

        pollingClient.sendMessage(workingMessage, List.of((ClientEvent event, AgentCard card) -> {
            if (event instanceof TaskEvent taskEvent) {
                taskIdRef.set(taskEvent.getTask().id());
                workingLatch.countDown();
            } else if (event instanceof TaskUpdateEvent tue && tue.getUpdateEvent() instanceof TaskStatusUpdateEvent status) {
                if (status.status().state() == TaskState.WORKING) {
                    taskIdRef.set(status.taskId());
                    workingLatch.countDown();
                }
            }
        }), (Throwable error) -> {
            workingLatch.countDown();
        });

        assertTrue(workingLatch.await(15, TimeUnit.SECONDS), "Task creation timed out");
        String createdTaskId = taskIdRef.get();
        assertNotNull(createdTaskId, "Task should be created");
        assertEquals(taskId, createdTaskId);

        // Set up streaming resubscription to listen for the QueueClosedEvent
        CountDownLatch streamCompletedLatch = new CountDownLatch(1);
        AtomicBoolean streamCompleted = new AtomicBoolean(false);
        AtomicBoolean streamErrored = new AtomicBoolean(false);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Create consumer - we expect the stream to complete when QueueClosedEvent arrives
        BiConsumer<ClientEvent, AgentCard> consumer = (event, agentCard) -> {
            // We might receive some events before the stream completes, that's fine
            // The important thing is that the stream eventually completes
        };

        // Create error handler that captures completion
        Consumer<Throwable> errorHandler = error -> {
            if (error == null) {
                // null error means stream completed normally
                streamCompleted.set(true);
            } else {
                streamErrored.set(true);
                errorRef.set(error);
            }
            streamCompletedLatch.countDown();
        };

        // Subscribe to the task - this creates a streaming subscription
        streamingClient.subscribeToTask(new TaskIdParams(taskId), List.of(consumer), errorHandler);

        // Wait for the EventConsumer to start polling (replaces unreliable Thread.sleep)
        // This ensures the consumer is ready to receive the QueueClosedEvent
        EventQueue queue = queueManager.get(taskId);
        assertNotNull(queue, "Queue should exist for task " + taskId);
        queueManager.awaitQueuePollerStart(queue);

        // Now manually send a QueueClosedEvent to Kafka to simulate queue closure on another node
        QueueClosedEvent closedEvent = new QueueClosedEvent(taskId);
        ReplicatedEventQueueItem replicatedClosedEvent = new ReplicatedEventQueueItem(taskId, closedEvent);
        String eventJson = JsonUtil.toJson(replicatedClosedEvent);

        // Send to Kafka using reactive messaging
        testEmitter.send(eventJson);

        // Wait for the stream to complete - should happen when QueueClosedEvent is received
        // Allow extra time for Kafka message processing and CDI event propagation
        assertTrue(streamCompletedLatch.await(30, TimeUnit.SECONDS),
                "Streaming subscription should complete when QueueClosedEvent is received");

        // Verify the stream completed normally (not with an error)
        if (!streamCompleted.get()) {
            LOGGER.error("Stream did not complete normally! streamErrored={}, errorRef={}",
                    streamErrored.get(), errorRef.get(), errorRef.get());
        }
        assertTrue(streamCompleted.get(), "Stream should complete normally when QueueClosedEvent is received");
        assertFalse(streamErrored.get(), "Stream should not error on QueueClosedEvent");
        assertNull(errorRef.get(), "Should not receive error when stream completes gracefully");
    }

    @Test
    public void testPoisonPillGenerationOnTaskFinalization() throws Exception {
        String taskId = "poison-pill-gen-test-" + System.currentTimeMillis();
        String contextId = "test-context-" + System.currentTimeMillis();

        // Clear any previous events
        testConsumer.clear();

        // Create a task that will be completed (finalized)
        Message completeMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("complete")))
                .taskId(taskId)
                .messageId("complete-msg-" + System.currentTimeMillis())
                .contextId(contextId)
                .build();

        CountDownLatch completeLatch = new CountDownLatch(1);
        AtomicReference<Task> finalTask = new AtomicReference<>();

        nonStreamingClient.sendMessage(completeMessage, List.of((ClientEvent event, AgentCard card) -> {
            if (event instanceof TaskEvent taskEvent) {
                finalTask.set(taskEvent.getTask());
                // Count down for any task event - we just need the task to be created
                completeLatch.countDown();
            }
        }), (Throwable error) -> {
            completeLatch.countDown();
        });

        assertTrue(completeLatch.await(15, TimeUnit.SECONDS), "Task creation timed out");
        Task createdTask = finalTask.get();
        assertNotNull(createdTask, "Task should be created");

        // The task should complete very quickly since it's a simple operation
        // Wait a moment to ensure all events have been enqueued
        Thread.sleep(2000);

        // Wait for the QueueClosedEvent to be published to Kafka
        // The event should be published automatically because isTaskFinalized() returns true
        ReplicatedEventQueueItem poisonPill = testConsumer.waitForClosedEvent(taskId, 30);
        assertNotNull(poisonPill, "QueueClosedEvent should be published to Kafka when task is finalized");
        assertTrue(poisonPill.isClosedEvent(), "Event should be marked as closedEvent");
        assertEquals(taskId, poisonPill.getTaskId(), "QueueClosedEvent should have correct task ID");

        // Verify the event is actually a QueueClosedEvent
        assertInstanceOf(QueueClosedEvent.class, poisonPill.getEvent(),
                "Event should be a QueueClosedEvent");
        QueueClosedEvent closedEvent =
                (QueueClosedEvent) poisonPill.getEvent();
        assertEquals(taskId, closedEvent.getTaskId(), "QueueClosedEvent task ID should match");
    }

    /**
     * Checks if an error is a benign stream closed/cancelled error that should be ignored.
     * HTTP/2 streams can be cancelled during normal cleanup, which is not an actual error.
     *
     * @param error the throwable to check (may be null)
     * @return true if this is a benign stream closure error that should be ignored
     */
    private boolean isStreamClosedError(Throwable error) {
        return isStreamClosedError(error, new HashSet<>());
    }

    /**
     * Internal recursive implementation with cycle detection to prevent infinite recursion.
     *
     * @param error the throwable to check
     * @param visited set of already-visited throwables to detect cycles
     * @return true if this is a benign stream closure error
     */
    private boolean isStreamClosedError(Throwable error, Set<Throwable> visited) {
        if (error == null || !visited.add(error)) {
            // Null or already visited (cycle detected)
            return false;
        }

        // Check for IOException which includes stream cancellation
        if (error instanceof IOException) {
            String message = error.getMessage();
            if (message != null) {
                // Filter out normal stream closure/cancellation errors
                if (message.contains("Stream closed") ||
                    message.contains("Stream") && message.contains("cancelled") ||
                    message.contains("EOF reached") ||
                    message.contains("CANCEL")) {
                    return true;
                }
            }
        }

        // Check cause recursively with cycle detection
        Throwable cause = error.getCause();
        if (cause != null) {
            return isStreamClosedError(cause, visited);
        }

        return false;
    }

}