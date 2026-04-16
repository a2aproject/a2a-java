package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.spec.Task;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test for {@link TaskMapper_v0_3} to verify conversion of fully populated Task objects
 * between v0.3 and v1.0 protocol versions.
 */
class TaskMapper_v0_3_Test {

    @Test
    void testFullyPopulatedTaskConversion() {
        // Create a fully populated v0.3 Task
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Message_v0_3 v03Message = new Message_v0_3(
            Message_v0_3.Role.USER,
            List.of(new TextPart_v0_3("Hello, agent!")),
            "msg-001",
            "ctx-001",
            "task-001",
            List.of("ref-task-001"),
            Map.of("key", "value"),
            List.of("ext1")
        );

        TaskStatus_v0_3 v03Status = new TaskStatus_v0_3(
            TaskState_v0_3.WORKING,
            v03Message,
            now
        );

        Artifact_v0_3 v03Artifact = new Artifact_v0_3(
            "artifact-001",
            "Test Artifact",
            "A test artifact",
            List.of(
                new TextPart_v0_3("Response text"),
                new DataPart_v0_3(Map.of("result", "success"))
            ),
            Map.of("artifactMeta", "value"),
            List.of("artifactExt")
        );

        Message_v0_3 v03HistoryMessage = new Message_v0_3(
            Message_v0_3.Role.AGENT,
            List.of(new TextPart_v0_3("Agent response")),
            "msg-002",
            "ctx-001",
            "task-001",
            null,
            null,
            null
        );

        Task_v0_3 v03Task = new Task_v0_3(
            "task-001",
            "ctx-001",
            v03Status,
            List.of(v03Artifact),
            List.of(v03HistoryMessage),
            Map.of("taskMeta", "taskValue")
        );

        // Convert v0.3 → v1.0
        Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(v03Task);

        // Verify v1.0 Task
        assertNotNull(v10Task);
        assertEquals("task-001", v10Task.id());
        assertEquals("ctx-001", v10Task.contextId());
        assertEquals(Map.of("taskMeta", "taskValue"), v10Task.metadata());

        // Verify status conversion
        assertNotNull(v10Task.status());
        assertEquals(org.a2aproject.sdk.spec.TaskState.TASK_STATE_WORKING, v10Task.status().state());
        assertEquals(now, v10Task.status().timestamp());

        // Verify status message conversion
        assertNotNull(v10Task.status().message());
        assertEquals(org.a2aproject.sdk.spec.Message.Role.ROLE_USER, v10Task.status().message().role());
        assertEquals("msg-001", v10Task.status().message().messageId());
        assertEquals("ctx-001", v10Task.status().message().contextId());
        assertEquals("task-001", v10Task.status().message().taskId());
        assertEquals(1, v10Task.status().message().parts().size());
        assertEquals("Hello, agent!", ((org.a2aproject.sdk.spec.TextPart) v10Task.status().message().parts().get(0)).text());

        // Verify artifacts conversion
        assertNotNull(v10Task.artifacts());
        assertEquals(1, v10Task.artifacts().size());
        org.a2aproject.sdk.spec.Artifact v10Artifact = v10Task.artifacts().get(0);
        assertEquals("artifact-001", v10Artifact.artifactId());
        assertEquals("Test Artifact", v10Artifact.name());
        assertEquals("A test artifact", v10Artifact.description());
        assertEquals(2, v10Artifact.parts().size());
        assertEquals("Response text", ((org.a2aproject.sdk.spec.TextPart) v10Artifact.parts().get(0)).text());

        // Verify history conversion
        assertNotNull(v10Task.history());
        assertEquals(1, v10Task.history().size());
        org.a2aproject.sdk.spec.Message v10HistoryMsg = v10Task.history().get(0);
        assertEquals(org.a2aproject.sdk.spec.Message.Role.ROLE_AGENT, v10HistoryMsg.role());
        assertEquals("msg-002", v10HistoryMsg.messageId());
        assertEquals("Agent response", ((org.a2aproject.sdk.spec.TextPart) v10HistoryMsg.parts().get(0)).text());

        // Convert v1.0 → v0.3 (round trip)
        Task_v0_3 v03TaskRoundTrip = TaskMapper_v0_3.INSTANCE.fromV10(v10Task);

        // Verify round-trip conversion
        assertNotNull(v03TaskRoundTrip);
        assertEquals("task-001", v03TaskRoundTrip.getId());
        assertEquals("ctx-001", v03TaskRoundTrip.getContextId());
        assertEquals(TaskState_v0_3.WORKING, v03TaskRoundTrip.getStatus().state());
        assertEquals("msg-001", v03TaskRoundTrip.getStatus().message().getMessageId());
        assertEquals(1, v03TaskRoundTrip.getArtifacts().size());
        assertEquals("artifact-001", v03TaskRoundTrip.getArtifacts().get(0).artifactId());
        assertEquals(1, v03TaskRoundTrip.getHistory().size());
        assertEquals("msg-002", v03TaskRoundTrip.getHistory().get(0).getMessageId());
    }

    @Test
    void testMinimalTaskConversion() {
        // Test with minimal Task (no artifacts, no history, no metadata)
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        TaskStatus_v0_3 v03Status = new TaskStatus_v0_3(
            TaskState_v0_3.SUBMITTED
        );

        Task_v0_3 v03Task = new Task_v0_3(
            "task-minimal",
            "ctx-minimal",
            v03Status,
            null,
            null,
            null
        );

        // Convert v0.3 → v1.0
        Task v10Task = TaskMapper_v0_3.INSTANCE.toV10(v03Task);

        // Verify minimal conversion
        assertNotNull(v10Task);
        assertEquals("task-minimal", v10Task.id());
        assertEquals("ctx-minimal", v10Task.contextId());
        assertEquals(org.a2aproject.sdk.spec.TaskState.TASK_STATE_SUBMITTED, v10Task.status().state());
        assertNull(v10Task.status().message());

        // v1.0 Task compact constructor converts null to empty list
        assertNotNull(v10Task.artifacts());
        assertEquals(0, v10Task.artifacts().size());
        assertNotNull(v10Task.history());
        assertEquals(0, v10Task.history().size());
        assertNull(v10Task.metadata());

        // Round trip
        Task_v0_3 v03TaskRoundTrip = TaskMapper_v0_3.INSTANCE.fromV10(v10Task);
        assertNotNull(v03TaskRoundTrip);
        assertEquals("task-minimal", v03TaskRoundTrip.getId());
        assertEquals(TaskState_v0_3.SUBMITTED, v03TaskRoundTrip.getStatus().state());
    }

    @Test
    void testNullTaskConversion() {
        // Null safety
        assertNull(TaskMapper_v0_3.INSTANCE.toV10(null));
        assertNull(TaskMapper_v0_3.INSTANCE.fromV10(null));
    }
}
