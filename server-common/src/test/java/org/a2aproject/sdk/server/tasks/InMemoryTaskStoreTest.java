package org.a2aproject.sdk.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

public class InMemoryTaskStoreTest {

    @Test
    public void testSaveAndGet() {
        InMemoryTaskStore store = new InMemoryTaskStore();
        Task task = sampleTask("task-abc");

        store.save(task, false);

        Task retrieved = store.get(task.id());
        assertSame(task, retrieved);
    }

    @Test
    public void testGetNonExistent() {
        InMemoryTaskStore store = new InMemoryTaskStore();

        Task retrieved = store.get("nonexistent");
        assertNull(retrieved);
    }

    @Test
    public void testDelete() {
        InMemoryTaskStore store = new InMemoryTaskStore();
        Task task = sampleTask("task-abc");

        store.save(task, false);
        store.delete(task.id());

        Task retrieved = store.get(task.id());
        assertNull(retrieved);
    }

    @Test
    public void testDeleteNonExistent() {
        InMemoryTaskStore store = new InMemoryTaskStore();

        store.delete("non-existent");
    }

    @Test
    public void testListTransformsHistoryAndArtifacts() {
        InMemoryTaskStore store = new InMemoryTaskStore();
        Task task = Task.builder()
                .id("task-abc")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .history(List.of(sampleMessage("msg-1"), sampleMessage("msg-2")))
                .artifacts(List.of(sampleArtifact("artifact-1")))
                .metadata(Map.of("origin", "test"))
                .build();

        store.save(task, false);

        ListTasksParams params = ListTasksParams.builder().build();
        List<Task> tasks = store.list(params, null).tasks();

        assertEquals(1, tasks.size());
        Task listed = tasks.get(0);
        assertEquals(task.id(), listed.id());
        assertEquals(task.contextId(), listed.contextId());
        assertTrue(listed.history().isEmpty(), "Default list() should omit history");
        assertTrue(listed.artifacts().isEmpty(), "Default list() should omit artifacts");
        assertEquals(task.metadata(), listed.metadata());
    }

    private static Task sampleTask(String id) {
        return Task.builder()
                .id(id)
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
    }

    private static Message sampleMessage(String messageId) {
        return Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(List.of(new TextPart("content")))
                .messageId(messageId)
                .build();
    }

    private static Artifact sampleArtifact(String artifactId) {
        return Artifact.builder()
                .artifactId(artifactId)
                .parts(List.of(new TextPart("artifact content")))
                .build();
    }
}

