package io.a2a.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.a2a.spec.A2AClientInvalidArgsError;
import io.a2a.spec.A2AClientInvalidStateError;
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

public class ClientTaskManagerTest {

    private ClientTaskManager taskManager;
    private Task sampleTask;
    private Message sampleMessage;

    @BeforeEach
    public void setUp() {
        taskManager = new ClientTaskManager();
        
        sampleTask = Task.builder()
                .id("task123")
                .contextId("context456")
                .status(new TaskStatus(TaskState.WORKING))
                .build();
        
        sampleMessage = Message.builder()
                .messageId("msg1")
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("Hello")))
                .build();
    }

    @Test
    public void testGetCurrentTaskNoTaskRaisesError() {
        A2AClientInvalidStateError exception = assertThrows(
                A2AClientInvalidStateError.class,
                () -> taskManager.getCurrentTask()
        );
        assertTrue(exception.getMessage().contains("No current task"));
    }

    @Test
    public void testSaveTaskEventWithTask() throws Exception {
        Task result = taskManager.saveTaskEvent(sampleTask);
        
        assertEquals(sampleTask, taskManager.getCurrentTask());
        assertEquals(sampleTask, result);
    }

    @Test
    public void testSaveTaskEventWithTaskAlreadySetRaisesError() throws Exception {
        taskManager.saveTaskEvent(sampleTask);
        
        A2AClientInvalidArgsError exception = assertThrows(
                A2AClientInvalidArgsError.class,
                () -> taskManager.saveTaskEvent(sampleTask)
        );
        assertTrue(exception.getMessage().contains("Task is already set, create new manager for new tasks."));
    }

    @Test
    public void testSaveTaskEventWithStatusUpdate() throws Exception {
        taskManager.saveTaskEvent(sampleTask);
        
        TaskStatusUpdateEvent statusUpdate = TaskStatusUpdateEvent.builder()
                .taskId(sampleTask.id())
                .contextId(sampleTask.contextId())
                .status(new TaskStatus(TaskState.COMPLETED, sampleMessage, null))
                .build();
        
        Task updatedTask = taskManager.saveTaskEvent(statusUpdate);
        
        assertEquals(TaskState.COMPLETED, updatedTask.status().state());
        assertNotNull(updatedTask.history());
        assertEquals(1, updatedTask.history().size());
        assertEquals(sampleMessage, updatedTask.history().get(0));
    }

    @Test
    public void testSaveTaskEventWithStatusUpdateAndMetadata() throws Exception {
        taskManager.saveTaskEvent(sampleTask);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 42);
        
        TaskStatusUpdateEvent statusUpdate = TaskStatusUpdateEvent.builder()
                .taskId(sampleTask.id())
                .contextId(sampleTask.contextId())
                .status(new TaskStatus(TaskState.WORKING))
                .metadata(metadata)
                .build();
        
        Task updatedTask = taskManager.saveTaskEvent(statusUpdate);
        
        assertNotNull(updatedTask.metadata());
        assertEquals("value1", updatedTask.metadata().get("key1"));
        assertEquals(42, updatedTask.metadata().get("key2"));
    }

    @Test
    public void testSaveTaskEventWithArtifactUpdate() throws Exception {
        taskManager.saveTaskEvent(sampleTask);
        
        Artifact artifact = Artifact.builder()
                .artifactId("art1")
                .parts(Collections.singletonList(new TextPart("artifact content")))
                .build();
        
        TaskArtifactUpdateEvent artifactUpdate = TaskArtifactUpdateEvent.builder()
                .taskId(sampleTask.id())
                .contextId(sampleTask.contextId())
                .artifact(artifact)
                .build();
        
        Task updatedTask = taskManager.saveTaskEvent(artifactUpdate);
        
        assertNotNull(updatedTask);
        assertNotNull(updatedTask.artifacts());
        assertEquals(1, updatedTask.artifacts().size());
        assertEquals("art1", updatedTask.artifacts().get(0).artifactId());
    }

    @Test
    public void testSaveTaskEventCreatesTaskIfNotExists() throws Exception {
        TaskStatusUpdateEvent statusUpdate = TaskStatusUpdateEvent.builder()
                .taskId("new_task")
                .contextId("new_context")
                .status(new TaskStatus(TaskState.WORKING))
                .build();
        
        Task updatedTask = taskManager.saveTaskEvent(statusUpdate);
        
        assertNotNull(updatedTask);
        assertEquals("new_task", updatedTask.id());
        assertEquals("new_context", updatedTask.contextId());
        assertEquals(TaskState.WORKING, updatedTask.status().state());
    }

    @Test
    public void testSaveTaskEventCreatesTaskFromArtifactUpdateIfNotExists() {
        Artifact artifact = Artifact.builder()
                .artifactId("art1")
                .parts(Collections.singletonList(new TextPart("artifact content")))
                .build();
        
        TaskArtifactUpdateEvent artifactUpdate = TaskArtifactUpdateEvent.builder()
                .taskId("new_task_id")
                .contextId("new_context_id")
                .artifact(artifact)
                .build();
        
        Task updatedTask = taskManager.saveTaskEvent(artifactUpdate);
        
        assertNotNull(updatedTask);
        assertEquals("new_task_id", updatedTask.id());
        assertEquals("new_context_id", updatedTask.contextId());
        assertNotNull(updatedTask.artifacts());
        assertEquals(1, updatedTask.artifacts().size());
    }

    @Test
    public void testUpdateWithMessage() {
        // Use a task with mutable history list initialized properly
        Task taskWithHistory = Task.builder()
                .id("task123")
                .contextId("context456")
                .status(new TaskStatus(TaskState.WORKING))
                .build();
        
        Task updatedTask = taskManager.updateWithMessage(sampleMessage, taskWithHistory);
        
        assertNotNull(updatedTask.history());
        assertEquals(1, updatedTask.history().size());
        assertEquals(sampleMessage, updatedTask.history().get(0));
    }

    @Test
    public void testUpdateWithMessageMovesStatusMessage() {
        Message statusMessage = Message.builder()
                .messageId("status_msg")
                .role(Message.Role.AGENT)
                .parts(Collections.singletonList(new TextPart("Status")))
                .build();
        
        Task taskWithStatusMessage = Task.builder()
                .id("task123")
                .contextId("context456")
                .status(new TaskStatus(TaskState.WORKING, statusMessage, null))
                .build();
        
        Task updatedTask = taskManager.updateWithMessage(sampleMessage, taskWithStatusMessage);
        
        assertNotNull(updatedTask.history());
        assertEquals(2, updatedTask.history().size());
        assertEquals(statusMessage, updatedTask.history().get(0));
        assertEquals(sampleMessage, updatedTask.history().get(1));
        assertNull(updatedTask.status().message());
    }

    @Test
    public void testUpdateWithMessagePreservesExistingHistory() {
        Message existingMessage = Message.builder()
                .messageId("existing_msg")
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("Existing")))
                .build();
        
        Task taskWithHistory = Task.builder()
                .id("task123")
                .contextId("context456")
                .status(new TaskStatus(TaskState.WORKING))
                .history(List.of(existingMessage))
                .build();
        
        Task updatedTask = taskManager.updateWithMessage(sampleMessage, taskWithHistory);
        
        assertNotNull(updatedTask.history());
        assertEquals(2, updatedTask.history().size());
        assertEquals(existingMessage, updatedTask.history().get(0));
        assertEquals(sampleMessage, updatedTask.history().get(1));
    }

    @Test
    public void testSaveTaskEventMultipleStatusUpdates() throws Exception {
        taskManager.saveTaskEvent(sampleTask);
        
        // First status update
        TaskStatusUpdateEvent statusUpdate1 = TaskStatusUpdateEvent.builder()
                .taskId(sampleTask.id())
                .contextId(sampleTask.contextId())
                .status(new TaskStatus(TaskState.WORKING, sampleMessage, null))
                .build();
        
        Task updatedTask1 = taskManager.saveTaskEvent(statusUpdate1);
        assertEquals(TaskState.WORKING, updatedTask1.status().state());
        assertEquals(1, updatedTask1.history().size());
        
        // Second status update
        Message secondMessage = Message.builder()
                .messageId("msg2")
                .role(Message.Role.AGENT)
                .parts(Collections.singletonList(new TextPart("Second message")))
                .build();
        
        TaskStatusUpdateEvent statusUpdate2 = TaskStatusUpdateEvent.builder()
                .taskId(sampleTask.id())
                .contextId(sampleTask.contextId())
                .status(new TaskStatus(TaskState.COMPLETED, secondMessage, null))
                .build();
        
        Task updatedTask2 = taskManager.saveTaskEvent(statusUpdate2);
        assertEquals(TaskState.COMPLETED, updatedTask2.status().state());
        assertEquals(2, updatedTask2.history().size());
    }

    @Test
    public void testSaveTaskEventMultipleArtifactUpdates() throws Exception {
        taskManager.saveTaskEvent(sampleTask);
        
        // First artifact update
        Artifact artifact1 = Artifact.builder()
                .artifactId("art1")
                .parts(Collections.singletonList(new TextPart("First artifact")))
                .build();
        
        TaskArtifactUpdateEvent artifactUpdate1 = TaskArtifactUpdateEvent.builder()
                .taskId(sampleTask.id())
                .contextId(sampleTask.contextId())
                .artifact(artifact1)
                .build();
        
        Task updatedTask1 = taskManager.saveTaskEvent(artifactUpdate1);
        assertEquals(1, updatedTask1.artifacts().size());
        
        // Second artifact update
        Artifact artifact2 = Artifact.builder()
                .artifactId("art2")
                .parts(Collections.singletonList(new TextPart("Second artifact")))
                .build();
        
        TaskArtifactUpdateEvent artifactUpdate2 = TaskArtifactUpdateEvent.builder()
                .taskId(sampleTask.id())
                .contextId(sampleTask.contextId())
                .artifact(artifact2)
                .build();
        
        Task updatedTask2 = taskManager.saveTaskEvent(artifactUpdate2);
        assertEquals(2, updatedTask2.artifacts().size());
    }

    @Test
    public void testSaveTaskEventWithEmptyContextId() throws Exception {
        TaskStatusUpdateEvent statusUpdate = TaskStatusUpdateEvent.builder()
                .taskId("task_with_empty_context")
                .contextId("")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        
        Task updatedTask = taskManager.saveTaskEvent(statusUpdate);
        
        assertNotNull(updatedTask);
        assertEquals("task_with_empty_context", updatedTask.id());
        assertEquals("", updatedTask.contextId());
    }
}
