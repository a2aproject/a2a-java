package io.a2a.extras.queuemanager.replicated.mp_reactive;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.enterprise.event.Event;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.a2a.extras.queuemanager.replicated.core.ReplicatedEventQueueItem;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.util.Utils;

@ExtendWith(MockitoExtension.class)
class ReactiveMessagingReplicationStrategyTest {

    @Mock
    private Emitter<String> emitter;

    @Mock
    private Event<ReplicatedEventQueueItem> cdiEvent;

    @InjectMocks
    private ReactiveMessagingReplicationStrategy strategy;

    private StreamingEventKind testEvent;

    @BeforeEach
    public void setUp() {
        testEvent = new TaskStatusUpdateEvent.Builder()
                .taskId("test-task")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .isFinal(false)
                .build();
    }

    private String createValidJsonMessage(String taskId, String contextId) throws Exception {
        // Create a proper ReplicatedEventQueueItem JSON with StreamingEventKind
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(taskId)
                .contextId(contextId)
                .status(new TaskStatus(TaskState.WORKING))
                .isFinal(false)
                .build();
        ReplicatedEventQueueItem replicatedEvent = new ReplicatedEventQueueItem(taskId, event);
        return Utils.OBJECT_MAPPER.writeValueAsString(replicatedEvent);
    }

    @Test
    public void testSendCallsEmitter() {
        String taskId = "test-task-123";

        strategy.send(taskId, testEvent);

        // Verify that emitter.send was called (don't care about exact JSON format)
        verify(emitter).send(any(String.class));
    }

    @Test
    public void testSendPropagatesEmitterExceptions() {
        String taskId = "test-task-456";
        RuntimeException emitterException = new RuntimeException("Failed to send replicated event");

        doThrow(emitterException).when(emitter).send(any(String.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                strategy.send(taskId, testEvent)
        );

        // The implementation wraps the original exception, so check the cause
        assertEquals(emitterException, exception.getCause());
        assertEquals("Failed to send replicated event", exception.getMessage());
    }

    @Test
    public void testOnReplicatedEventWithValidJson() throws Exception {
        String validJsonMessage = createValidJsonMessage("test-task-101", "test-context");

        // Should not throw - valid JSON should be handled gracefully
        assertDoesNotThrow(() -> strategy.onReplicatedEvent(validJsonMessage));

        // Note: We test the actual CDI firing in integration tests
        // Unit tests here focus on error handling and basic flow
    }


    @Test
    public void testOnReplicatedEventHandlesInvalidJson() {
        String invalidJsonMessage = "invalid-json";

        // Should not throw - invalid JSON should be handled gracefully
        assertDoesNotThrow(() -> strategy.onReplicatedEvent(invalidJsonMessage));

        // CDI event should not be fired for invalid JSON
        verify(cdiEvent, never()).fire(any(ReplicatedEventQueueItem.class));
    }


}