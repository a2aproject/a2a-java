package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.HashMap;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
