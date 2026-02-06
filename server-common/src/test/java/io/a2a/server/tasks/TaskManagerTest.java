package io.a2a.server.tasks;

import static io.a2a.jsonrpc.common.json.JsonUtil.fromJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.a2a.spec.A2AServerException;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskManagerTest {
    private static final String TASK_JSON = """
            {
                "id": "task-abc",
                "contextId" : "session-xyz",
                "status": {"state": "submitted"}
            }""";

    Task minimalTask;
    TaskStore taskStore;
    TaskManager taskManager;

    @BeforeEach
    public void init() throws Exception {
        minimalTask = fromJson(TASK_JSON, Task.class);
        taskStore = new InMemoryTaskStore();
        taskManager = new TaskManager(minimalTask.id(), minimalTask.contextId(), taskStore, null);
    }

    @Test
    public void testGetTaskExisting() {
        Task expectedTask = minimalTask;
        taskStore.save(expectedTask, false);
        Task retrieved = taskManager.getTask();
        assertSame(expectedTask, retrieved);
    }

    @Test
    public void testGetTaskNonExistent() {
        Task retrieved = taskManager.getTask();
        assertNull(retrieved);
    }

    @Test
    public void testSaveTaskEventNewTask() throws A2AServerException {
        taskManager.saveTaskEvent(minimalTask, false);
        Task saved = taskManager.getTask();
        Task retrieved = taskManager.getTask();
        assertSame(minimalTask, retrieved);
    }

    @Test
    public void testSaveTaskEventStatusUpdate() throws A2AServerException {
        Task initialTask = minimalTask;
        taskStore.save(initialTask, false);

        TaskStatus newStatus = new TaskStatus(
                TaskState.WORKING,
                Message.builder()
                        .role(Message.Role.AGENT)
                        .parts(Collections.singletonList(new TextPart("content")))
                        .messageId("messageId")
                        .build(),
                null);
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent(
                minimalTask.id(),
                newStatus,
                minimalTask.contextId(),
                false,
                new HashMap<>());


        taskManager.saveTaskEvent(event, false);
        Task saved = taskManager.getTask();
        Task updated = taskManager.getTask();

        assertNotSame(initialTask, updated);

        assertEquals(initialTask.id(), updated.id());
        assertEquals(initialTask.contextId(), updated.contextId());
        // TODO type does not get unmarshalled
        //assertEquals(initialTask.getType(), updated.getType());
        assertSame(newStatus, updated.status());
    }

    @Test
    public void testSaveTaskEventArtifactUpdate() throws A2AServerException {
        Task initialTask = minimalTask;
        Artifact newArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content")))
                .build();
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(newArtifact)
                .build();
        taskManager.saveTaskEvent(event, false);
        Task saved = taskManager.getTask();

        Task updatedTask = taskManager.getTask();

        assertNotSame(initialTask, updatedTask);
        assertEquals(initialTask.id(), updatedTask.id());
        assertEquals(initialTask.contextId(), updatedTask.contextId());
        assertSame(initialTask.status().state(), updatedTask.status().state());
        assertEquals(1, updatedTask.artifacts().size());
        assertEquals(newArtifact, updatedTask.artifacts().get(0));
    }

    @Test
    public void testEnsureTaskExisting() {
        // This tests the 'working case' of the internal logic to check a task being updated existas
        // We are already testing that
    }

    @Test
    public void testEnsureTaskNonExistentForStatusUpdate() throws A2AServerException {
        // Tests that an update event instantiates a new task and that
        TaskManager taskManagerWithoutId = new TaskManager(null, null, taskStore, null);
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("new-task")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        taskManagerWithoutId.saveTaskEvent(event, false);
        Task task = taskManagerWithoutId.getTask();
        assertEquals(event.taskId(), taskManagerWithoutId.getTaskId());
        assertEquals(event.contextId(), taskManagerWithoutId.getContextId());

        Task newTask = taskManagerWithoutId.getTask();
        assertEquals(event.taskId(), newTask.id());
        assertEquals(event.contextId(), newTask.contextId());
        assertEquals(TaskState.SUBMITTED, newTask.status().state());
        assertSame(newTask, task);
    }

    @Test
    public void testSaveTaskEventNewTaskNoTaskId() throws A2AServerException {
        TaskManager taskManagerWithoutId = new TaskManager(null, null, taskStore, null);
        Task task = Task.builder()
                .id("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        taskManagerWithoutId.saveTaskEvent(task, false);
        Task saved = taskManager.getTask();
        assertEquals(task.id(), taskManagerWithoutId.getTaskId());
        assertEquals(task.contextId(), taskManagerWithoutId.getContextId());

        Task retrieved = taskManagerWithoutId.getTask();
        assertSame(task, retrieved);
    }

    @Test
    public void testGetTaskNoTaskId() {
        TaskManager taskManagerWithoutId = new TaskManager(null, null, taskStore, null);
        Task retrieved = taskManagerWithoutId.getTask();
        assertNull(retrieved);
    }

    // Additional tests for missing coverage scenarios

    @Test
    public void testTaskArtifactUpdateEventAppendTrueWithExistingArtifact() throws A2AServerException {
        // Setup: Create a task with an existing artifact
        Task initialTask = minimalTask;
        Artifact existingArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("existing content")))
                .metadata(Map.of("key1", "value1"))
                .build();
        Task taskWithArtifact = Task.builder(initialTask)
                .artifacts(Collections.singletonList(existingArtifact))
                .build();
        taskStore.save(taskWithArtifact, false);

        // Test: Append new parts to existing artifact
        Artifact newArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("new content")))
                .metadata(Map.of("key2", "value2"))
                .build();
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(newArtifact)
                .append(true)
                .build();

        taskManager.saveTaskEvent(event, false);
        Task updatedTask = taskManager.getTask();

        assertEquals(1, updatedTask.artifacts().size());
        Artifact updatedArtifact = updatedTask.artifacts().get(0);
        assertEquals("artifact-id", updatedArtifact.artifactId());
        assertEquals(2, updatedArtifact.parts().size());
        assertEquals("existing content", ((TextPart) updatedArtifact.parts().get(0)).text());
        assertEquals("new content", ((TextPart) updatedArtifact.parts().get(1)).text());

        assertEquals("value1", updatedArtifact.metadata().get("key1"));
        assertEquals("value2", updatedArtifact.metadata().get("key2"));
    }

    @Test
    public void testTaskArtifactUpdateEventAppendTrueWithoutExistingArtifact() throws A2AServerException {
        // Setup: Create a task without artifacts
        Task initialTask = minimalTask;
        taskStore.save(initialTask, false);

        // Test: Try to append to non-existent artifact (should be ignored)
        Artifact newArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("new content")))
                .build();
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(newArtifact)
                .append(true)
                .build();

        taskManager.saveTaskEvent(event, false);
        Task saved = taskManager.getTask();
        Task updatedTask = taskManager.getTask();

        // Should have no artifacts since append was ignored
        assertEquals(0, updatedTask.artifacts().size());
    }

    @Test
    public void testTaskArtifactUpdateEventAppendFalseWithExistingArtifact() throws A2AServerException {
        // Setup: Create a task with an existing artifact
        Task initialTask = minimalTask;
        Artifact existingArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("existing content")))
                .build();
        Task taskWithArtifact = Task.builder(initialTask)
                .artifacts(Collections.singletonList(existingArtifact))
                .build();
        taskStore.save(taskWithArtifact, false);

        // Test: Replace existing artifact (append=false)
        Artifact newArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("replacement content")))
                .build();
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(newArtifact)
                .append(false)
                .build();

        taskManager.saveTaskEvent(event, false);
        Task saved = taskManager.getTask();
        Task updatedTask = taskManager.getTask();

        assertEquals(1, updatedTask.artifacts().size());
        Artifact updatedArtifact = updatedTask.artifacts().get(0);
        assertEquals("artifact-id", updatedArtifact.artifactId());
        assertEquals(1, updatedArtifact.parts().size());
        assertEquals("replacement content", ((TextPart) updatedArtifact.parts().get(0)).text());
    }

    @Test
    public void testTaskArtifactUpdateEventAppendNullWithExistingArtifact() throws A2AServerException {
        // Setup: Create a task with an existing artifact
        Task initialTask = minimalTask;
        Artifact existingArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("existing content")))
                .build();
        Task taskWithArtifact = Task.builder(initialTask)
                .artifacts(Collections.singletonList(existingArtifact))
                .build();
        taskStore.save(taskWithArtifact, false);

        // Test: Replace existing artifact (append=null, defaults to false)
        Artifact newArtifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("replacement content")))
                .build();
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(newArtifact)
                .build(); // append is null

        taskManager.saveTaskEvent(event, false);
        Task saved = taskManager.getTask();
        Task updatedTask = taskManager.getTask();

        assertEquals(1, updatedTask.artifacts().size());
        Artifact updatedArtifact = updatedTask.artifacts().get(0);
        assertEquals("artifact-id", updatedArtifact.artifactId());
        assertEquals(1, updatedArtifact.parts().size());
        assertEquals("replacement content", ((TextPart) updatedArtifact.parts().get(0)).text());
    }

    @Test
    public void testAddingTaskWithDifferentIdFails() {
        // Test that adding a task with a different id from the taskmanager's taskId fails
        TaskManager taskManagerWithId = new TaskManager("task-abc", "session-xyz", taskStore, null);
        
        Task differentTask = Task.builder()
                .id("different-task-id")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        assertThrows(A2AServerException.class, () -> {
            taskManagerWithId.saveTaskEvent(differentTask, false);
        });
    }

    @Test
    public void testAddingTaskWithDifferentIdViaStatusUpdateFails() {
        // Test that adding a status update with different taskId fails
        TaskManager taskManagerWithId = new TaskManager("task-abc", "session-xyz", taskStore, null);
        
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("different-task-id")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        assertThrows(A2AServerException.class, () -> {
            taskManagerWithId.saveTaskEvent(event, false);
        });
    }

    @Test
    public void testAddingTaskWithDifferentIdViaArtifactUpdateFails() {
        // Test that adding an artifact update with different taskId fails
        TaskManager taskManagerWithId = new TaskManager("task-abc", "session-xyz", taskStore, null);
        
        Artifact artifact = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content")))
                .build();
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId("different-task-id")
                .contextId("session-xyz")
                .artifact(artifact)
                .build();

        assertThrows(A2AServerException.class, () -> {
            taskManagerWithId.saveTaskEvent(event, false);
        });
    }

    @Test
    public void testTaskWithNoMessageUsesInitialMessage() throws A2AServerException {
        // Test that adding a task with no message, and there is a TaskManager.initialMessage, 
        // the initialMessage gets used
        Message initialMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("initial message")))
                .messageId("initial-msg-id")
                .build();
        
        TaskManager taskManagerWithInitialMessage = new TaskManager(null, null, taskStore, initialMessage);
        
        // Use a status update event instead of a Task to trigger createTask
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        taskManagerWithInitialMessage.saveTaskEvent(event, false);
        Task saved = taskManager.getTask();
        Task retrieved = taskManagerWithInitialMessage.getTask();

        // Check that the task has the initial message in its history
        assertNotNull(retrieved.history());
        assertEquals(1, retrieved.history().size());
        Message historyMessage = retrieved.history().get(0);
        assertEquals(initialMessage.messageId(), historyMessage.messageId());
        assertEquals(initialMessage.role(), historyMessage.role());
        assertEquals("initial message", ((TextPart) historyMessage.parts().get(0)).text());
    }

    @Test
    public void testTaskWithMessageDoesNotUseInitialMessage() throws A2AServerException {
        // Test that adding a task with a message does not use the initial message
        Message initialMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("initial message")))
                .messageId("initial-msg-id")
                .build();
        
        TaskManager taskManagerWithInitialMessage = new TaskManager(null, null, taskStore, initialMessage);
        
        Message taskMessage = Message.builder()
                .role(Message.Role.AGENT)
                .parts(Collections.singletonList(new TextPart("task message")))
                .messageId("task-msg-id")
                .build();
        
        // Use TaskStatusUpdateEvent to trigger the creation of a task, which will check if the initialMessage is used.
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED, taskMessage, null))
                .build();

        taskManagerWithInitialMessage.saveTaskEvent(event, false);
        Task saved = taskManager.getTask();
        Task retrieved = taskManagerWithInitialMessage.getTask();

        // There should now be a history containing the initialMessage
        // But the current message (taskMessage) should be in the state, not in the history
        assertNotNull(retrieved.history());
        assertEquals(1, retrieved.history().size());
        assertEquals("initial message", ((TextPart) retrieved.history().get(0).parts().get(0)).text());
        
        // The message in the current state should be taskMessage
        assertNotNull(retrieved.status().message());
        assertEquals("task message", ((TextPart) retrieved.status().message().parts().get(0)).text());
    }

    @Test
    public void testMultipleArtifactsWithSameArtifactId() throws A2AServerException {
        // Test handling of multiple artifacts with the same artifactId
        Task initialTask = minimalTask;
        taskStore.save(initialTask, false);

        // Add first artifact
        Artifact artifact1 = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content 1")))
                .build();
        TaskArtifactUpdateEvent event1 = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(artifact1)
                .build();
        taskManager.saveTaskEvent(event1, false);

        // Add second artifact with same artifactId (should replace the first)
        Artifact artifact2 = Artifact.builder()
                .artifactId("artifact-id")
                .name("artifact-2")
                .parts(Collections.singletonList(new TextPart("content 2")))
                .build();
        TaskArtifactUpdateEvent event2 = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(artifact2)
                .build();
        taskManager.saveTaskEvent(event2, false);

        Task updatedTask = taskManager.getTask();
        assertEquals(1, updatedTask.artifacts().size());
        Artifact finalArtifact = updatedTask.artifacts().get(0);
        assertEquals("artifact-id", finalArtifact.artifactId());
        assertEquals("artifact-2", finalArtifact.name());
        assertEquals("content 2", ((TextPart) finalArtifact.parts().get(0)).text());
    }

    @Test
    public void testMultipleArtifactsWithDifferentArtifactIds() throws A2AServerException {
        // Test handling of multiple artifacts with different artifactIds
        Task initialTask = minimalTask;
        taskStore.save(initialTask, false);

        // Add first artifact
        Artifact artifact1 = Artifact.builder()
                .artifactId("artifact-id-1")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content 1")))
                .build();
        TaskArtifactUpdateEvent event1 = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(artifact1)
                .build();
        taskManager.saveTaskEvent(event1, false);

        // Add second artifact with different artifactId (should be added)
        Artifact artifact2 = Artifact.builder()
                .artifactId("artifact-id-2")
                .name("artifact-2")
                .parts(Collections.singletonList(new TextPart("content 2")))
                .build();
        TaskArtifactUpdateEvent event2 = TaskArtifactUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .artifact(artifact2)
                .build();
        taskManager.saveTaskEvent(event2, false);

        Task updatedTask = taskManager.getTask();
        assertEquals(2, updatedTask.artifacts().size());
        
        // Verify both artifacts are present
        List<Artifact> artifacts = updatedTask.artifacts();
        assertTrue(artifacts.stream()
                .anyMatch(a -> "artifact-id-1".equals(a.artifactId()) 
                        && "content 1".equals(((TextPart) a.parts().get(0)).text()))
                , "Artifact 1 should be present");
        assertTrue(artifacts.stream()
                .anyMatch(a -> "artifact-id-2".equals(a.artifactId()) 
                && "content 2".equals(((TextPart) a.parts().get(0)).text()))
                , "Artifact 2 should be present");
    }

    @Test
    public void testInvalidTaskIdValidation() {
        // Test that creating TaskManager with null taskId is allowed (Python allows None)
        TaskManager taskManagerWithNullId = new TaskManager(null, "context", taskStore, null);
        assertNull(taskManagerWithNullId.getTaskId());

        // Test that empty string task ID is handled (Java doesn't have explicit validation like Python)
        TaskManager taskManagerWithEmptyId = new TaskManager("", "context", taskStore, null);
        assertEquals("", taskManagerWithEmptyId.getTaskId());
    }

    @Test
    public void testSaveTaskEventMetadataUpdate() throws A2AServerException {
        // Test that metadata from TaskStatusUpdateEvent gets saved to the task
        Task initialTask = minimalTask;
        taskStore.save(initialTask, false);

        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("meta_key_test", "meta_value_test");

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .status(new TaskStatus(TaskState.WORKING))
                .metadata(newMetadata)
                .build();

        taskManager.saveTaskEvent(event, false);

        Task updatedTask = taskManager.getTask();
        assertEquals(newMetadata, updatedTask.metadata());
    }

    @Test
    public void testSaveTaskEventMetadataUpdateNull() throws A2AServerException {
        // Test that null metadata in TaskStatusUpdateEvent doesn't affect task
        Task initialTask = minimalTask;
        taskStore.save(initialTask, false);

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .status(new TaskStatus(TaskState.WORKING))
                .metadata(null)
                .build();

        taskManager.saveTaskEvent(event, false);

        Task updatedTask = taskManager.getTask();
        // Should preserve original task's metadata (which is likely null for minimal task)
        assertNull(updatedTask.metadata());
    }

    @Test
    public void testSaveTaskEventMetadataMergeExisting() throws A2AServerException {
        // Test that metadata update merges with existing metadata
        Map<String, Object> originalMetadata = new HashMap<>();
        originalMetadata.put("original_key", "original_value");
        
        Task taskWithMetadata = Task.builder(minimalTask)
                .metadata(originalMetadata)
                .build();
        taskStore.save(taskWithMetadata, false);

        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("new_key", "new_value");

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId(minimalTask.id())
                .contextId(minimalTask.contextId())
                .status(new TaskStatus(TaskState.WORKING))
                .metadata(newMetadata)
                .build();

        taskManager.saveTaskEvent(event, false);

        Task updatedTask = taskManager.getTask();

        Map<String, Object> mergedMetadata = new HashMap<>(originalMetadata);
        mergedMetadata.putAll(newMetadata);
        assertEquals(mergedMetadata, updatedTask.metadata());
    }

    @Test
    public void testCreateTaskWithInitialMessage() throws A2AServerException {
        // Test equivalent of _init_task_obj functionality
        Message initialMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("initial message")))
                .messageId("initial-msg-id")
                .build();

        TaskManager taskManagerWithMessage = new TaskManager(null, null, taskStore, initialMessage);

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        taskManagerWithMessage.saveTaskEvent(event, false);
        Task savedTask = taskManagerWithMessage.getTask();

        // Verify task was created properly
        assertNotNull(savedTask);
        assertEquals("new-task-id", savedTask.id());
        assertEquals("some-context", savedTask.contextId());
        assertEquals(TaskState.SUBMITTED, savedTask.status().state());

        // Verify initial message is in history
        assertNotNull(savedTask.history());
        assertEquals(1, savedTask.history().size());
        Message historyMessage = savedTask.history().get(0);
        assertEquals("initial-msg-id", historyMessage.messageId());
        assertEquals("initial message", ((TextPart) historyMessage.parts().get(0)).text());
    }

    @Test
    public void testCreateTaskWithoutInitialMessage() throws A2AServerException {
        // Test task creation without initial message
        TaskManager taskManagerWithoutMessage = new TaskManager(null, null, taskStore, null);

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        taskManagerWithoutMessage.saveTaskEvent(event, false);
        Task savedTask = taskManagerWithoutMessage.getTask();

        // Verify task was created properly
        assertNotNull(savedTask);
        assertEquals("new-task-id", savedTask.id());
        assertEquals("some-context", savedTask.contextId());
        assertEquals(TaskState.SUBMITTED, savedTask.status().state());

        // Verify no history since there was no initial message
        assertTrue(savedTask.history().isEmpty());
    }

    @Test
    public void testSaveTaskInternal() throws A2AServerException {
        // Test equivalent of _save_task functionality through saveTaskEvent
        TaskManager taskManagerWithoutId = new TaskManager(null, null, taskStore, null);
        
        Task newTask = Task.builder()
                .id("test-task-id")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        taskManagerWithoutId.saveTaskEvent(newTask, false);
        Task savedTask = taskManagerWithoutId.getTask();

        // Verify internal state was updated
        assertEquals("test-task-id", taskManagerWithoutId.getTaskId());
        assertEquals("test-context", taskManagerWithoutId.getContextId());
        assertSame(savedTask, taskManagerWithoutId.getTask());
    }

    @Test
    public void testUpdateWithMessage() throws A2AServerException {
        Message initialMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("initial message")))
                .messageId("initial-msg-id")
                .build();

        TaskManager taskManagerWithInitialMessage = new TaskManager(null, null, taskStore, initialMessage);

        Message taskMessage = Message.builder()
                .role(Message.Role.AGENT)
                .parts(Collections.singletonList(new TextPart("task message")))
                .messageId("task-msg-id")
                .build();

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED, taskMessage, null))
                .build();

        taskManagerWithInitialMessage.saveTaskEvent(event, false);
        Task saved = taskManagerWithInitialMessage.getTask();

        Message updateMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("update message")))
                .messageId("update-msg-id")
                .build();

        Task updated = taskManagerWithInitialMessage.updateWithMessage(updateMessage, saved);

        // There should now be a history containing the initialMessage, task message and update message
        assertNotNull(updated.history());
        assertEquals(3, updated.history().size());
        assertEquals("initial message", ((TextPart) updated.history().get(0).parts().get(0)).text());

        // The message in the current state should be null
        assertNull(updated.status().message());
        assertEquals("task message", ((TextPart) updated.history().get(1).parts().get(0)).text());
        assertEquals("update message", ((TextPart) updated.history().get(2).parts().get(0)).text());
    }
}
