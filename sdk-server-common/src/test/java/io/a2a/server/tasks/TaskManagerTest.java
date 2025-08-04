package io.a2a.server.tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.a2a.spec.A2AServerException;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.util.Utils;

public class TaskManagerTest {
    private static final String TASK_JSON = """
            {
                "id": "task-abc",
                "contextId" : "session-xyz",
                "status": {"state": "submitted"},
                "kind": "task"
            }""";

    Task minimalTask;
    TaskStore taskStore;
    TaskManager taskManager;

    @BeforeEach
    public void init() throws Exception {
        minimalTask = Utils.unmarshalFrom(TASK_JSON, Task.TYPE_REFERENCE);
        taskStore = new InMemoryTaskStore();
        taskManager = new TaskManager(minimalTask.getId(), minimalTask.getContextId(), taskStore, null);
    }

    @Test
    public void testGetTaskExisting() {
        Task expectedTask = minimalTask;
        taskStore.save(expectedTask);
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
        Task saved = taskManager.saveTaskEvent(minimalTask);
        Task retrieved = taskManager.getTask();
        assertSame(minimalTask, retrieved);
        assertSame(retrieved, saved);
    }

    @Test
    public void testSaveTaskEventStatusUpdate() throws A2AServerException {
        Task initialTask = minimalTask;
        taskStore.save(initialTask);

        TaskStatus newStatus = new TaskStatus(
                TaskState.WORKING,
                new Message.Builder()
                        .role(Message.Role.AGENT)
                        .parts(Collections.singletonList(new TextPart("content")))
                        .messageId("messageId")
                        .build(),
                null);
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent(
                minimalTask.getId(),
                newStatus,
                minimalTask.getContextId(),
                false,
                new HashMap<>());


        Task saved = taskManager.saveTaskEvent(event);
        Task updated = taskManager.getTask();

        assertNotSame(initialTask, updated);
        assertSame(updated, saved);

        assertEquals(initialTask.getId(), updated.getId());
        assertEquals(initialTask.getContextId(), updated.getContextId());
        // TODO type does not get unmarshalled
        //assertEquals(initialTask.getType(), updated.getType());
        assertSame(newStatus, updated.getStatus());
    }

    @Test
    public void testSaveTaskEventArtifactUpdate() throws A2AServerException {
        Task initialTask = minimalTask;
        Artifact newArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content")))
                .build();
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(newArtifact)
                .build();
        Task saved = taskManager.saveTaskEvent(event);

        Task updatedTask = taskManager.getTask();
        assertSame(updatedTask, saved);

        assertNotSame(initialTask, updatedTask);
        assertEquals(initialTask.getId(), updatedTask.getId());
        assertEquals(initialTask.getContextId(), updatedTask.getContextId());
        assertSame(initialTask.getStatus().state(), updatedTask.getStatus().state());
        assertEquals(1, updatedTask.getArtifacts().size());
        assertEquals(newArtifact, updatedTask.getArtifacts().get(0));
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
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId("new-task")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .isFinal(false)
                .build();

        Task task = taskManagerWithoutId.saveTaskEvent(event);
        assertEquals(event.getTaskId(), taskManagerWithoutId.getTaskId());
        assertEquals(event.getContextId(), taskManagerWithoutId.getContextId());

        Task newTask = taskManagerWithoutId.getTask();
        assertEquals(event.getTaskId(), newTask.getId());
        assertEquals(event.getContextId(), newTask.getContextId());
        assertEquals(TaskState.SUBMITTED, newTask.getStatus().state());
        assertSame(newTask, task);
    }

    @Test
    public void testSaveTaskEventNewTaskNoTaskId() throws A2AServerException {
        TaskManager taskManagerWithoutId = new TaskManager(null, null, taskStore, null);
        Task task = new Task.Builder()
                .id("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        Task saved = taskManagerWithoutId.saveTaskEvent(task);
        assertEquals(task.getId(), taskManagerWithoutId.getTaskId());
        assertEquals(task.getContextId(), taskManagerWithoutId.getContextId());

        Task retrieved = taskManagerWithoutId.getTask();
        assertSame(task, retrieved);
        assertSame(retrieved, saved);
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
        Artifact existingArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("existing content")))
                .build();
        Task taskWithArtifact = new Task.Builder(initialTask)
                .artifacts(Collections.singletonList(existingArtifact))
                .build();
        taskStore.save(taskWithArtifact);

        // Test: Append new parts to existing artifact
        Artifact newArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("new content")))
                .build();
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(newArtifact)
                .append(true)
                .build();

        Task updatedTask = taskManager.saveTaskEvent(event);

        assertEquals(1, updatedTask.getArtifacts().size());
        Artifact updatedArtifact = updatedTask.getArtifacts().get(0);
        assertEquals("artifact-id", updatedArtifact.artifactId());
        assertEquals(2, updatedArtifact.parts().size());
        assertEquals("existing content", ((TextPart) updatedArtifact.parts().get(0)).getText());
        assertEquals("new content", ((TextPart) updatedArtifact.parts().get(1)).getText());
    }

    @Test
    public void testTaskArtifactUpdateEventAppendTrueWithoutExistingArtifact() throws A2AServerException {
        // Setup: Create a task without artifacts
        Task initialTask = minimalTask;
        taskStore.save(initialTask);

        // Test: Try to append to non-existent artifact (should be ignored)
        Artifact newArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("new content")))
                .build();
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(newArtifact)
                .append(true)
                .build();

        Task saved = taskManager.saveTaskEvent(event);
        Task updatedTask = taskManager.getTask();

        // Should have no artifacts since append was ignored
        assertEquals(0, updatedTask.getArtifacts().size());
    }

    @Test
    public void testTaskArtifactUpdateEventAppendFalseWithExistingArtifact() throws A2AServerException {
        // Setup: Create a task with an existing artifact
        Task initialTask = minimalTask;
        Artifact existingArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("existing content")))
                .build();
        Task taskWithArtifact = new Task.Builder(initialTask)
                .artifacts(Collections.singletonList(existingArtifact))
                .build();
        taskStore.save(taskWithArtifact);

        // Test: Replace existing artifact (append=false)
        Artifact newArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("replacement content")))
                .build();
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(newArtifact)
                .append(false)
                .build();

        Task saved = taskManager.saveTaskEvent(event);
        Task updatedTask = taskManager.getTask();

        assertEquals(1, updatedTask.getArtifacts().size());
        Artifact updatedArtifact = updatedTask.getArtifacts().get(0);
        assertEquals("artifact-id", updatedArtifact.artifactId());
        assertEquals(1, updatedArtifact.parts().size());
        assertEquals("replacement content", ((TextPart) updatedArtifact.parts().get(0)).getText());
    }

    @Test
    public void testTaskArtifactUpdateEventAppendNullWithExistingArtifact() throws A2AServerException {
        // Setup: Create a task with an existing artifact
        Task initialTask = minimalTask;
        Artifact existingArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("existing content")))
                .build();
        Task taskWithArtifact = new Task.Builder(initialTask)
                .artifacts(Collections.singletonList(existingArtifact))
                .build();
        taskStore.save(taskWithArtifact);

        // Test: Replace existing artifact (append=null, defaults to false)
        Artifact newArtifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("replacement content")))
                .build();
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(newArtifact)
                .build(); // append is null

        Task saved = taskManager.saveTaskEvent(event);
        Task updatedTask = taskManager.getTask();

        assertEquals(1, updatedTask.getArtifacts().size());
        Artifact updatedArtifact = updatedTask.getArtifacts().get(0);
        assertEquals("artifact-id", updatedArtifact.artifactId());
        assertEquals(1, updatedArtifact.parts().size());
        assertEquals("replacement content", ((TextPart) updatedArtifact.parts().get(0)).getText());
    }

    @Test
    public void testAddingTaskWithDifferentIdFails() {
        // Test that adding a task with a different id from the taskmanager's taskId fails
        TaskManager taskManagerWithId = new TaskManager("task-abc", "session-xyz", taskStore, null);
        
        Task differentTask = new Task.Builder()
                .id("different-task-id")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        assertThrows(A2AServerException.class, () -> {
            taskManagerWithId.saveTaskEvent(differentTask);
        });
    }

    @Test
    public void testAddingTaskWithDifferentIdViaStatusUpdateFails() {
        // Test that adding a status update with different taskId fails
        TaskManager taskManagerWithId = new TaskManager("task-abc", "session-xyz", taskStore, null);
        
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId("different-task-id")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.WORKING))
                .isFinal(false)
                .build();

        assertThrows(A2AServerException.class, () -> {
            taskManagerWithId.saveTaskEvent(event);
        });
    }

    @Test
    public void testAddingTaskWithDifferentIdViaArtifactUpdateFails() {
        // Test that adding an artifact update with different taskId fails
        TaskManager taskManagerWithId = new TaskManager("task-abc", "session-xyz", taskStore, null);
        
        Artifact artifact = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content")))
                .build();
        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId("different-task-id")
                .contextId("session-xyz")
                .artifact(artifact)
                .build();

        assertThrows(A2AServerException.class, () -> {
            taskManagerWithId.saveTaskEvent(event);
        });
    }

    @Test
    public void testTaskWithNoMessageUsesInitialMessage() throws A2AServerException {
        // Test that adding a task with no message, and there is a TaskManager.initialMessage, 
        // the initialMessage gets used
        Message initialMessage = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("initial message")))
                .messageId("initial-msg-id")
                .build();
        
        TaskManager taskManagerWithInitialMessage = new TaskManager(null, null, taskStore, initialMessage);
        
        // Use a status update event instead of a Task to trigger createTask
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .isFinal(false)
                .build();

        Task saved = taskManagerWithInitialMessage.saveTaskEvent(event);
        Task retrieved = taskManagerWithInitialMessage.getTask();

        // Check that the task has the initial message in its history
        assertNotNull(retrieved.getHistory());
        assertEquals(1, retrieved.getHistory().size());
        Message historyMessage = retrieved.getHistory().get(0);
        assertEquals(initialMessage.getMessageId(), historyMessage.getMessageId());
        assertEquals(initialMessage.getRole(), historyMessage.getRole());
        assertEquals("initial message", ((TextPart) historyMessage.getParts().get(0)).getText());
    }

    @Test
    public void testTaskWithMessageDoesNotUseInitialMessage() throws A2AServerException {
        // Test that adding a task with a message does not use the initial message
        Message initialMessage = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("initial message")))
                .messageId("initial-msg-id")
                .build();
        
        TaskManager taskManagerWithInitialMessage = new TaskManager(null, null, taskStore, initialMessage);
        
        Message taskMessage = new Message.Builder()
                .role(Message.Role.AGENT)
                .parts(Collections.singletonList(new TextPart("task message")))
                .messageId("task-msg-id")
                .build();
        
        // Use TaskStatusUpdateEvent to trigger the creation of a task, which will check if the initialMessage is used.
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId("new-task-id")
                .contextId("some-context")
                .status(new TaskStatus(TaskState.SUBMITTED, taskMessage, null))
                .isFinal(false)
                .build();

        Task saved = taskManagerWithInitialMessage.saveTaskEvent(event);
        Task retrieved = taskManagerWithInitialMessage.getTask();

        // There should now be a history containing the initialMessage
        // But the current message (taskMessage) should be in the state, not in the history
        assertNotNull(retrieved.getHistory());
        assertEquals(1, retrieved.getHistory().size());
        assertEquals("initial message", ((TextPart) retrieved.getHistory().get(0).getParts().get(0)).getText());
        
        // The message in the current state should be taskMessage
        assertNotNull(retrieved.getStatus().message());
        assertEquals("task message", ((TextPart) retrieved.getStatus().message().getParts().get(0)).getText());
    }

    @Test
    public void testMultipleArtifactsWithSameArtifactId() throws A2AServerException {
        // Test handling of multiple artifacts with the same artifactId
        Task initialTask = minimalTask;
        taskStore.save(initialTask);

        // Add first artifact
        Artifact artifact1 = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content 1")))
                .build();
        TaskArtifactUpdateEvent event1 = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(artifact1)
                .build();
        taskManager.saveTaskEvent(event1);

        // Add second artifact with same artifactId (should replace the first)
        Artifact artifact2 = new Artifact.Builder()
                .artifactId("artifact-id")
                .name("artifact-2")
                .parts(Collections.singletonList(new TextPart("content 2")))
                .build();
        TaskArtifactUpdateEvent event2 = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(artifact2)
                .build();
        taskManager.saveTaskEvent(event2);

        Task updatedTask = taskManager.getTask();
        assertEquals(1, updatedTask.getArtifacts().size());
        Artifact finalArtifact = updatedTask.getArtifacts().get(0);
        assertEquals("artifact-id", finalArtifact.artifactId());
        assertEquals("artifact-2", finalArtifact.name());
        assertEquals("content 2", ((TextPart) finalArtifact.parts().get(0)).getText());
    }

    @Test
    public void testMultipleArtifactsWithDifferentArtifactIds() throws A2AServerException {
        // Test handling of multiple artifacts with different artifactIds
        Task initialTask = minimalTask;
        taskStore.save(initialTask);

        // Add first artifact
        Artifact artifact1 = new Artifact.Builder()
                .artifactId("artifact-id-1")
                .name("artifact-1")
                .parts(Collections.singletonList(new TextPart("content 1")))
                .build();
        TaskArtifactUpdateEvent event1 = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(artifact1)
                .build();
        taskManager.saveTaskEvent(event1);

        // Add second artifact with different artifactId (should be added)
        Artifact artifact2 = new Artifact.Builder()
                .artifactId("artifact-id-2")
                .name("artifact-2")
                .parts(Collections.singletonList(new TextPart("content 2")))
                .build();
        TaskArtifactUpdateEvent event2 = new TaskArtifactUpdateEvent.Builder()
                .taskId(minimalTask.getId())
                .contextId(minimalTask.getContextId())
                .artifact(artifact2)
                .build();
        taskManager.saveTaskEvent(event2);

        Task updatedTask = taskManager.getTask();
        assertEquals(2, updatedTask.getArtifacts().size());
        
        // Verify both artifacts are present
        List<Artifact> artifacts = updatedTask.getArtifacts();
        assertTrue(artifacts.stream()
                .anyMatch(a -> "artifact-id-1".equals(a.artifactId()) 
                        && "content 1".equals(((TextPart) a.parts().get(0)).getText()))
                , "Artifact 1 should be present");
        assertTrue(artifacts.stream()
                .anyMatch(a -> "artifact-id-2".equals(a.artifactId()) 
                && "content 2".equals(((TextPart) a.parts().get(0)).getText()))
                , "Artifact 2 should be present");
    }
}
