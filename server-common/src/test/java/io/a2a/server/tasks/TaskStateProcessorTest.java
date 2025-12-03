package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TaskStateProcessorTest {

    private TaskStateProcessor processor;
    private static final String TASK_ID = "task-123";
    private static final String CONTEXT_ID = "context-456";

    @BeforeEach
    public void setUp() {
        processor = new TaskStateProcessor();
    }

    @Test
    public void testProcessEventWithTaskEvent() {
        // Given a Task event
        Task task = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        // When processing the event
        Task result = processor.processEvent(task, null);

        // Then the task is stored and returned
        assertNotNull(result);
        assertEquals(TASK_ID, result.getId());
        assertEquals(CONTEXT_ID, result.getContextId());
        assertEquals(TaskState.SUBMITTED, result.getStatus().state());
        assertNull(result.getStatus().message());

        // And can be retrieved
        Task retrieved = processor.getTask(TASK_ID);
        assertEquals(task, retrieved);
    }

    @Test
    public void testProcessEventWithTaskStatusUpdateEventOnNewTask() {
        // Given a TaskStatusUpdateEvent for a new task
        Message initialMessage = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("Hello")))
                .build();

        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        // When processing the event
        Task result = processor.processEvent(event, initialMessage);

        // Then a new task is created with the status
        assertNotNull(result);
        assertEquals(TASK_ID, result.getId());
        assertEquals(CONTEXT_ID, result.getContextId());
        assertEquals(TaskState.WORKING, result.getStatus().state());
        assertNotNull(result.getHistory());
        assertEquals(1, result.getHistory().size());
        assertEquals(initialMessage, result.getHistory().get(0));
    }

    @Test
    public void testProcessEventWithTaskStatusUpdateEventOnExistingTask() {
        // Given an existing task
        Task existingTask = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        processor.setTask(existingTask);

        // When processing a status update event
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        Task result = processor.processEvent(event, null);

        // Then the task status is updated
        assertNotNull(result);
        assertEquals(TaskState.WORKING, result.getStatus().state());
    }

    @Test
    public void testProcessEventWithTaskStatusUpdateEventWithMetadata() {
        // Given an existing task
        Map<String, Object> initialMetadata = new HashMap<>();
        initialMetadata.put("key1", "value1");

        Task existingTask = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .metadata(initialMetadata)
                .build();
        processor.setTask(existingTask);

        // When processing a status update with new metadata
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("key2", "value2");

        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.WORKING))
                .metadata(newMetadata)
                .build();

        Task result = processor.processEvent(event, null);

        // Then both metadata entries are present
        assertNotNull(result.getMetadata());
        assertEquals(2, result.getMetadata().size());
        assertEquals("value1", result.getMetadata().get("key1"));
        assertEquals("value2", result.getMetadata().get("key2"));
    }

    @Test
    public void testProcessEventWithTaskStatusUpdateEvent() {
        // Given a task with a message in its status
        Message statusMessage = new Message.Builder()
                .role(Message.Role.AGENT)
                .parts(List.of(new TextPart("Current message")))
                .build();

        Task existingTask = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.WORKING, statusMessage, OffsetDateTime.now(ZoneOffset.UTC)))
                .build();
        processor.setTask(existingTask);

        // When processing a status update
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();

        Task result = processor.processEvent(event, null);

        // Then the message is moved to history
        assertNotNull(result.getHistory());
        assertEquals(1, result.getHistory().size());
        assertEquals(statusMessage, result.getHistory().get(0));
        assertNull(result.getStatus().message());
    }

    @Test
    public void testProcessEventWithTaskArtifactUpdateEventOnNewTask() {
        // Given an artifact update event for a new task
        Message initialMessage = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("Hello")))
                .build();

        Artifact artifact = new Artifact.Builder()
                .artifactId("artifact-1")
                .name("test.txt")
                .parts(new TextPart("this is a text"))
                .build();

        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(TASK_ID)
                .contextId(CONTEXT_ID)
                .artifact(artifact)
                .build();

        // When processing the event
        Task result = processor.processEvent(event, initialMessage);

        // Then a new task is created with the artifact
        assertNotNull(result);
        assertEquals(TASK_ID, result.getId());
        assertNotNull(result.getArtifacts());
        assertEquals(1, result.getArtifacts().size());
        assertEquals(artifact, result.getArtifacts().get(0));
    }

    @Test
    public void testProcessEventWithTaskArtifactUpdateEventOnExistingTask() {
        // Given an existing task with an artifact
        Artifact existingArtifact = new Artifact.Builder()
                .artifactId("artifact-1")
                .name("old.txt")
                .parts(new TextPart("this is a text"))
                .build();

        Task existingTask = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.WORKING))
                .artifacts(List.of(existingArtifact))
                .build();
        processor.setTask(existingTask);

        // When processing an artifact update
        Artifact newArtifact = new Artifact.Builder()
                .artifactId("artifact-2")
                .name("new.txt")
                .parts(new TextPart("this is a new text"))
                .build();

        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(TASK_ID)
                .contextId(CONTEXT_ID)
                .artifact(newArtifact)
                .build();

        Task result = processor.processEvent(event, null);

        // Then both artifacts are present
        assertNotNull(result.getArtifacts());
        assertEquals(2, result.getArtifacts().size());
    }

    @Test
    public void testProcessEventWithUnknownEventType() {
        // Given an unknown event type
        Event unknownEvent = new Event() {
            // Anonymous implementation
        };

        // When processing the event
        Task result = processor.processEvent(unknownEvent, null);

        // Then null is returned
        assertNull(result);
    }

    @Test
    public void testAddMessageToHistory() {
        // Given an existing task
        Task existingTask = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        processor.setTask(existingTask);

        // When adding a message to history
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("New message")))
                .build();

        Task result = processor.addMessageToHistory(TASK_ID, message);

        // Then the message is in the history
        assertNotNull(result);
        assertNotNull(result.getHistory());
        assertEquals(1, result.getHistory().size());
        assertEquals(message, result.getHistory().get(0));
    }

    @Test
    public void testAddMessageToHistoryWithExistingStatusMessage() {
        // Given a task with a message in its status
        Message statusMessage = new Message.Builder()
                .role(Message.Role.AGENT)
                .parts(List.of(new TextPart("Status message")))
                .build();

        Task existingTask = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.WORKING, statusMessage, OffsetDateTime.now(ZoneOffset.UTC)))
                .build();
        processor.setTask(existingTask);

        // When adding a new message to history
        Message newMessage = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("New message")))
                .build();

        Task result = processor.addMessageToHistory(TASK_ID, newMessage);

        // Then both messages are in history and status message is cleared
        assertNotNull(result.getHistory());
        assertEquals(2, result.getHistory().size());
        assertEquals(statusMessage, result.getHistory().get(0));
        assertEquals(newMessage, result.getHistory().get(1));
        assertNull(result.getStatus().message());
    }

    @Test
    public void testAddMessageToHistoryWithNonExistentTask() {
        // When adding a message to a non-existent task
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("Message")))
                .build();

        Task result = processor.addMessageToHistory("non-existent", message);

        // Then null is returned
        assertNull(result);
    }

    @Test
    public void testGetTask() {
        // Given a task in the processor
        Task task = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        processor.setTask(task);

        // When getting the task
        Task result = processor.getTask(TASK_ID);

        // Then the task is returned
        assertNotNull(result);
        assertEquals(task, result);
    }

    @Test
    public void testGetTaskWithNonExistent() {
        // When getting a non-existent task
        Task result = processor.getTask("non-existent");

        // Then null is returned
        assertNull(result);
    }

    @Test
    public void testGetTaskWithNullTaskId() {
        // When getting a task with null ID
        Task result = processor.getTask(null);

        // Then null is returned
        assertNull(result);
    }

    @Test
    public void testSetTask() {
        // Given a task
        Task task = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        // When setting the task
        processor.setTask(task);

        // Then the task can be retrieved
        Task result = processor.getTask(TASK_ID);
        assertEquals(task, result);
    }

    @Test
    public void testSetTaskWithNull() {
        // When setting null task
        processor.setTask(null);

        // Then nothing happens (no exception)
        // This is a no-op test to ensure null safety
    }

    @Test
    public void testRemoveTask() {
        // Given a task in the processor
        Task task = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        processor.setTask(task);

        // When removing the task
        processor.removeTask(TASK_ID);

        // Then the task is no longer retrievable
        Task result = processor.getTask(TASK_ID);
        assertNull(result);
    }

    @Test
    public void testRemoveTaskWithNonExistent() {
        // When removing a non-existent task
        processor.removeTask("non-existent");

        // Then nothing happens (no exception)
        // This is a no-op test to ensure safe removal
    }

    @Test
    public void testConcurrentTaskManagement() {
        // Test that multiple tasks can be managed independently
        Task task1 = new Task.Builder()
                .id("task-1")
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        Task task2 = new Task.Builder()
                .id("task-2")
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        // When setting multiple tasks
        processor.setTask(task1);
        processor.setTask(task2);

        // Then both can be retrieved independently
        assertEquals(task1, processor.getTask("task-1"));
        assertEquals(task2, processor.getTask("task-2"));

        // When removing one task
        processor.removeTask("task-1");

        // Then only the other remains
        assertNull(processor.getTask("task-1"));
        assertNotNull(processor.getTask("task-2"));
    }

    @Test
    public void testTaskUpdate() {
        // Given an initial task
        Task initialTask = new Task.Builder()
                .id(TASK_ID)
                .contextId(CONTEXT_ID)
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        processor.setTask(initialTask);

        // When updating the task
        Task updatedTask = new Task.Builder(initialTask)
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();
        processor.setTask(updatedTask);

        // Then the updated version is retrieved
        Task result = processor.getTask(TASK_ID);
        assertEquals(TaskState.COMPLETED, result.getStatus().state());
    }
}
