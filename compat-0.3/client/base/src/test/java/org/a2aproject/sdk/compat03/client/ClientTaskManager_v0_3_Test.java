package org.a2aproject.sdk.compat03.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

public class ClientTaskManager_v0_3_Test {

    private static Artifact_v0_3 artifact(String text, Map<String, Object> metadata) {
        List<Part_v0_3<?>> parts = List.of(new TextPart_v0_3(text));
        return new Artifact_v0_3.Builder()
                .artifactId("artifact-1")
                .parts(parts)
                .metadata(metadata)
                .build();
    }

    @Test
    public void testArtifactChunkMetadataIsMergedIntoTheAccumulatedTask() {
        ClientTaskManager_v0_3 taskManager = new ClientTaskManager_v0_3();

        taskManager.saveTaskEvent(new TaskArtifactUpdateEvent_v0_3.Builder()
                .taskId("task-123")
                .contextId("context-123")
                .append(false)
                .artifact(artifact("chunk-1", Map.of("source", "ocr")))
                .build());

        Task_v0_3 task = taskManager.saveTaskEvent(new TaskArtifactUpdateEvent_v0_3.Builder()
                .taskId("task-123")
                .contextId("context-123")
                .append(true)
                .artifact(artifact("chunk-2", Map.of("progress", "60%")))
                .build());

        assertEquals(1, task.artifacts().size());
        assertEquals(
                Map.of("source", "ocr", "progress", "60%"),
                task.artifacts().get(0).metadata());
        assertEquals(2, task.artifacts().get(0).parts().size());
    }

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
