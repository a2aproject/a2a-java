package org.a2aproject.sdk.compat03.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

public class ClientTaskManager_v0_3_Test {

    @Test
    public void testStatusMessagesMoveToHistoryWhenSuperseded() throws Exception {
        ClientTaskManager_v0_3 taskManager = new ClientTaskManager_v0_3();
        Message_v0_3 workingMessage = new Message_v0_3.Builder()
                .messageId("working-message")
                .role(Message_v0_3.Role.AGENT)
                .parts(new TextPart_v0_3("working"))
                .build();
        Message_v0_3 completedMessage = new Message_v0_3.Builder()
                .messageId("completed-message")
                .role(Message_v0_3.Role.AGENT)
                .parts(new TextPart_v0_3("completed"))
                .build();

        Task_v0_3 workingTask = taskManager.saveTaskEvent(new TaskStatusUpdateEvent_v0_3.Builder()
                .taskId("task-123")
                .contextId("context-123")
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING, workingMessage, null))
                .build());

        assertTrue(workingTask.history().isEmpty());
        assertEquals(workingMessage, workingTask.status().message());

        Task_v0_3 completedTask = taskManager.saveTaskEvent(new TaskStatusUpdateEvent_v0_3.Builder()
                .taskId("task-123")
                .contextId("context-123")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED, completedMessage, null))
                .isFinal(true)
                .build());

        assertEquals(List.of(workingMessage), completedTask.history());
        assertEquals(completedMessage, completedTask.status().message());
    }
}
