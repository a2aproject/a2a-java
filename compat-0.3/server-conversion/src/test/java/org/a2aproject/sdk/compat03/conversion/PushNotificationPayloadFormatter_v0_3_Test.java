package org.a2aproject.sdk.compat03.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PushNotificationPayloadFormatter_v0_3_Test {

    private PushNotificationPayloadFormatter_v0_3 formatter;

    @BeforeEach
    void setUp() {
        formatter = new PushNotificationPayloadFormatter_v0_3();
    }

    @Test
    void targetVersionIs03() {
        assertEquals(A2AProtocol_v0_3.PROTOCOL_VERSION, formatter.targetVersion());
    }

    @Test
    void formatsTaskEventAsV03Task() {
        Task task = Task.builder()
                .id("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .build();

        String payload = formatter.formatPayload(task, task);

        assertNotNull(payload);
        assertTrue(payload.contains("\"kind\":\"task\""));
        assertTrue(payload.contains("\"id\":\"t1\""));
        assertTrue(payload.contains("\"status\""));
    }

    @Test
    void formatsStatusUpdateUsingTaskSnapshot() {
        Task snapshot = Task.builder()
                .id("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();

        String payload = formatter.formatPayload(event, snapshot);

        assertNotNull(payload);
        assertTrue(payload.contains("\"kind\":\"task\""));
        assertTrue(payload.contains("\"id\":\"t1\""));
    }

    @Test
    void skipsMessageEvents() {
        Task snapshot = Task.builder()
                .id("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();

        Message message = Message.builder()
                .messageId("m1")
                .role(Message.Role.ROLE_AGENT)
                .parts(new TextPart("hello"))
                .build();

        String payload = formatter.formatPayload(message, snapshot);

        assertNull(payload);
    }

    @Test
    void returnsNullWhenSnapshotIsNull() {
        Task task = Task.builder()
                .id("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                .build();

        String payload = formatter.formatPayload(task, null);

        assertNull(payload);
    }
}
