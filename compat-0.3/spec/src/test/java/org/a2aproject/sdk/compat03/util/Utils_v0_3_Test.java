package org.a2aproject.sdk.compat03.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Utils_v0_3#appendArtifactToTask(Task_v0_3, TaskArtifactUpdateEvent_v0_3, String)},
 * focused on how the metadata of a streamed chunk is carried over to the accumulated artifact.
 */
class Utils_v0_3_Test {

    private static final String TASK_ID = "task-1";
    private static final String CONTEXT_ID = "context-1";
    private static final String ARTIFACT_ID = "artifact-1";

    private static Task_v0_3 emptyTask() {
        return new Task_v0_3.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus_v0_3(TaskState_v0_3.WORKING))
                .build();
    }

    private static Artifact_v0_3 artifact(String text, Map<String, Object> metadata) {
        List<Part_v0_3<?>> parts = List.of(new TextPart_v0_3(text));
        Artifact_v0_3.Builder builder =
                new Artifact_v0_3.Builder().artifactId(ARTIFACT_ID).parts(parts);
        if (metadata != null) {
            builder.metadata(metadata);
        }
        return builder.build();
    }

    private static TaskArtifactUpdateEvent_v0_3 event(boolean append, Artifact_v0_3 artifact) {
        return new TaskArtifactUpdateEvent_v0_3(TASK_ID, artifact, CONTEXT_ID, append, false, null);
    }

    private static Artifact_v0_3 onlyArtifact(Task_v0_3 task) {
        assertEquals(1, task.artifacts().size());
        return task.artifacts().get(0);
    }

    @Test
    void appendChunkMergesItsMetadataIntoTheExistingArtifact() {
        Task_v0_3 task = emptyTask();
        task = Utils_v0_3.appendArtifactToTask(
                task,
                event(false, artifact("chunk-1", Map.of("source", "ocr", "page", 1))),
                TASK_ID);
        task = Utils_v0_3.appendArtifactToTask(
                task,
                event(true, artifact("chunk-2", Map.of("page", 2, "progress", "60%"))),
                TASK_ID);

        Artifact_v0_3 merged = onlyArtifact(task);
        Map<String, Object> metadata = merged.metadata();

        assertNotNull(metadata, "metadata must not be dropped when appending a chunk");
        assertEquals("ocr", metadata.get("source"), "keys only present on the first chunk must survive");
        assertEquals(2, metadata.get("page"), "keys set by the chunk must win over the earlier value");
        assertEquals("60%", metadata.get("progress"), "keys only present on the chunk must be added");
        assertEquals(2, merged.parts().size(), "the chunk's parts must still be appended");
    }

    @Test
    void appendChunkWithoutMetadataKeepsTheExistingMetadata() {
        Task_v0_3 task = Utils_v0_3.appendArtifactToTask(
                emptyTask(), event(false, artifact("chunk-1", Map.of("source", "ocr"))), TASK_ID);
        task = Utils_v0_3.appendArtifactToTask(task, event(true, artifact("chunk-2", null)), TASK_ID);

        Artifact_v0_3 merged = onlyArtifact(task);
        assertEquals(Map.of("source", "ocr"), merged.metadata());
        assertEquals(2, merged.parts().size());
    }

    @Test
    void appendChunkCarriesMetadataWhenTheExistingArtifactHasNone() {
        Task_v0_3 task = Utils_v0_3.appendArtifactToTask(
                emptyTask(), event(false, artifact("chunk-1", null)), TASK_ID);
        task = Utils_v0_3.appendArtifactToTask(
                task, event(true, artifact("chunk-2", Map.of("progress", "50%"))), TASK_ID);

        Artifact_v0_3 merged = onlyArtifact(task);
        assertEquals(Map.of("progress", "50%"), merged.metadata());
        assertEquals(2, merged.parts().size());
    }

    @Test
    void appendChunkLeavesTheExistingMetadataUntouchedWhenNeitherSideHasAny() {
        Task_v0_3 task = Utils_v0_3.appendArtifactToTask(
                emptyTask(), event(false, artifact("chunk-1", null)), TASK_ID);
        task = Utils_v0_3.appendArtifactToTask(task, event(true, artifact("chunk-2", null)), TASK_ID);

        Artifact_v0_3 merged = onlyArtifact(task);
        assertNull(merged.metadata());
        assertEquals(2, merged.parts().size());
    }

    @Test
    void appendChunkDoesNotMutateTheMetadataMapOfTheReceivedChunk() {
        Map<String, Object> firstChunkMetadata = new HashMap<>(Map.of("source", "ocr"));
        Map<String, Object> secondChunkMetadata = new HashMap<>(Map.of("progress", "60%"));

        Task_v0_3 task = Utils_v0_3.appendArtifactToTask(
                emptyTask(), event(false, artifact("chunk-1", firstChunkMetadata)), TASK_ID);
        task = Utils_v0_3.appendArtifactToTask(
                task, event(true, artifact("chunk-2", secondChunkMetadata)), TASK_ID);

        assertEquals(Map.of("source", "ocr"), firstChunkMetadata);
        assertEquals(Map.of("progress", "60%"), secondChunkMetadata);
    }

    @Test
    void replacementChunkReplacesTheArtifactInsteadOfMergingIt() {
        Task_v0_3 task = Utils_v0_3.appendArtifactToTask(
                emptyTask(),
                event(false, artifact("chunk-1", Map.of("source", "ocr", "page", 1))),
                TASK_ID);
        task = Utils_v0_3.appendArtifactToTask(
                task, event(false, artifact("chunk-2", Map.of("page", 9))), TASK_ID);

        Artifact_v0_3 replaced = onlyArtifact(task);
        assertEquals(Map.of("page", 9), replaced.metadata());
        assertEquals(1, replaced.parts().size());
    }

    @Test
    void appendChunkForUnknownArtifactIsIgnored() {
        Task_v0_3 task = Utils_v0_3.appendArtifactToTask(
                emptyTask(), event(true, artifact("chunk-1", Map.of("progress", "60%"))), TASK_ID);

        assertTrue(task.artifacts().isEmpty());
    }
}
