package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.Test;

class TaskStatusUpdateEventMapper_v0_3_Test {

    @Test
    void toV10_preservesMetadataAndDefensiveCopy() {
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("source", "sensor");

        TaskStatusUpdateEvent_v0_3 v03 = new TaskStatusUpdateEvent_v0_3.Builder()
                .taskId("task-123")
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING))
                .contextId("context-456")
                .isFinal(false)
                .metadata(mutableMetadata)
                .build();

        TaskStatusUpdateEvent v10 = TaskStatusUpdateEventMapper_v0_3.INSTANCE.toV10(v03);
        mutableMetadata.put("write", "should-not-leak");

        assertEquals("task-123", v10.taskId());
        assertEquals(TaskState.TASK_STATE_WORKING, v10.status().state());
        assertEquals("context-456", v10.contextId());
        assertEquals(Map.of("source", "sensor"), v10.metadata());
        assertThrows(UnsupportedOperationException.class, () -> v10.metadata().put("another", "value"));
    }

    @Test
    void fromV10_preservesMetadata() {
        TaskStatusUpdateEvent v10 = new TaskStatusUpdateEvent(
                "task-123",
                new TaskStatus(TaskState.TASK_STATE_WORKING),
                "context-456",
                Map.of("source", "sensor"));

        TaskStatusUpdateEvent_v0_3 v03 = TaskStatusUpdateEventMapper_v0_3.INSTANCE.fromV10(v10);

        assertEquals("task-123", v03.taskId());
        assertEquals(TaskState_v0_3.WORKING, v03.status().state());
        assertEquals("context-456", v03.contextId());
        assertEquals(Map.of("source", "sensor"), v03.metadata());
    }
}
