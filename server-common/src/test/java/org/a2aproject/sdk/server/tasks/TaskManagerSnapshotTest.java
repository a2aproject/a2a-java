package org.a2aproject.sdk.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskManagerSnapshotTest {

    private InMemoryTaskStore taskStore;

    @BeforeEach
    void setUp() {
        taskStore = new InMemoryTaskStore();
    }

    @Test
    void snapshotCapturedForTaskEvent() throws Exception {
        Task task = Task.builder()
                .id("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();

        AtomicReference<Task> snapshot = new AtomicReference<>();
        TaskManager tm = new TaskManager("t1", "c1", taskStore, null);
        tm.process(task, false, snapshot);

        assertNotNull(snapshot.get());
        assertEquals("t1", snapshot.get().id());
    }

    @Test
    void snapshotCapturedForStatusUpdateEvent() throws Exception {
        // Seed a task first
        Task task = Task.builder()
                .id("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
        taskStore.save(task, false);

        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();

        AtomicReference<Task> snapshot = new AtomicReference<>();
        TaskManager tm = new TaskManager("t1", "c1", taskStore, null);
        tm.process(event, false, snapshot);

        assertNotNull(snapshot.get());
        assertEquals(TaskState.TASK_STATE_WORKING, snapshot.get().status().state());
    }

    @Test
    void snapshotNullWhenNotProvided() throws Exception {
        Task task = Task.builder()
                .id("t1").contextId("c1")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();

        TaskManager tm = new TaskManager("t1", "c1", taskStore, null);
        // Old signature still works, no snapshot
        tm.process(task, false);

        // Just confirm the old method doesn't crash
        assertNotNull(taskStore.get("t1"));
    }
}
