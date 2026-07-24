package org.a2aproject.sdk.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TaskStatusUpdateEventTest {

    private static final String TASK_ID = "task-123";
    private static final String CONTEXT_ID = "context-456";

    @Test
    void testConstruction_withAllFields() {
        Map<String, Object> metadata = Map.of("source", "sensor");
        TaskStatus status = new TaskStatus(TaskState.TASK_STATE_WORKING);

        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent(TASK_ID, status, CONTEXT_ID, metadata);

        assertEquals(TASK_ID, event.taskId());
        assertEquals(status, event.status());
        assertEquals(CONTEXT_ID, event.contextId());
        assertEquals(metadata, event.metadata());
    }

    @Test
    void testConstruction_withNullMetadata() {
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent(
                TASK_ID,
                new TaskStatus(TaskState.TASK_STATE_WORKING),
                CONTEXT_ID,
                null);

        assertNull(event.metadata());
    }

    @Test
    void testConstruction_nullTaskId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new TaskStatusUpdateEvent(null, new TaskStatus(TaskState.TASK_STATE_WORKING), CONTEXT_ID, Map.of()));
    }

    @Test
    void testConstruction_nullStatus_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new TaskStatusUpdateEvent(TASK_ID, null, CONTEXT_ID, Map.of()));
    }

    @Test
    void testConstruction_nullContextId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new TaskStatusUpdateEvent(TASK_ID, new TaskStatus(TaskState.TASK_STATE_WORKING), null, Map.of()));
    }

    @Test
    void testMetadataImmutability() {
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("source", "sensor");

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId(TASK_ID)
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .contextId(CONTEXT_ID)
                .metadata(mutableMetadata)
                .build();

        mutableMetadata.put("write", "should-not-leak");

        assertEquals(Map.of("source", "sensor"), event.metadata());
        assertThrows(UnsupportedOperationException.class, () -> event.metadata().put("another", "value"));
    }

    @Test
    void testConstruction_preservesNullMetadataValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foo", null);

        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent(
                TASK_ID,
                new TaskStatus(TaskState.TASK_STATE_WORKING),
                CONTEXT_ID,
                metadata);

        assertEquals(1, event.metadata().size());
        assertTrue(event.metadata().containsKey("foo"));
        assertNull(event.metadata().get("foo"));
        assertThrows(UnsupportedOperationException.class, () -> event.metadata().put("bar", "baz"));
    }

    @Test
    void testBuilderFromExistingEvent_keepsEqualValues() {
        TaskStatusUpdateEvent original = new TaskStatusUpdateEvent(
                TASK_ID,
                new TaskStatus(TaskState.TASK_STATE_WORKING),
                CONTEXT_ID,
                Map.of("source", "sensor"));

        TaskStatusUpdateEvent copied = TaskStatusUpdateEvent.builder(original).build();

        assertEquals(original, copied);
        assertEquals(original.hashCode(), copied.hashCode());
        assertNotEquals(original, null);
    }
}
