package io.a2a.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.HashMap;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class JpaDatabaseTaskStoreTest {

    @Inject
    TaskStore taskStore;


    @Inject
    jakarta.persistence.EntityManager entityManager;

    @Test
    public void testIsJpaDatabaseTaskStore() {
        assertInstanceOf(JpaDatabaseTaskStore.class, taskStore);
    }

    @Test
    @Transactional
    public void testSaveAndRetrieveTask() {
        // Create a test task
        Task task = new Task.Builder()
                .id("test-task-1")
                .contextId("test-context-1")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .metadata(new HashMap<>())
                .build();

        // Save the task
        taskStore.save(task);

        // Retrieve the task
        Task retrieved = taskStore.get("test-task-1");
        
        assertNotNull(retrieved);
        assertEquals("test-task-1", retrieved.getId());
        assertEquals("test-context-1", retrieved.getContextId());
        assertEquals(TaskState.SUBMITTED, retrieved.getStatus().state());
    }

    @Test
    @Transactional
    public void testSaveAndRetrieveTaskWithHistory() {
        // Create a message for the task history
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("Hello, agent!")))
                .messageId("msg-1")
                .build();

        // Create a task with history
        Task task = new Task.Builder()
                .id("test-task-2")
                .contextId("test-context-2")
                .status(new TaskStatus(TaskState.WORKING))
                .history(Collections.singletonList(message))
                .build();

        // Save the task
        taskStore.save(task);

        // Retrieve the task
        Task retrieved = taskStore.get("test-task-2");
        
        assertNotNull(retrieved);
        assertEquals("test-task-2", retrieved.getId());
        assertEquals("test-context-2", retrieved.getContextId());
        assertEquals(TaskState.WORKING, retrieved.getStatus().state());
        assertEquals(1, retrieved.getHistory().size());
        assertEquals("msg-1", retrieved.getHistory().get(0).getMessageId());
        assertEquals("Hello, agent!", ((TextPart) retrieved.getHistory().get(0).getParts().get(0)).getText());
    }

    @Test
    @Transactional
    public void testUpdateExistingTask() {
        // Create and save initial task
        Task initialTask = new Task.Builder()
                .id("test-task-3")
                .contextId("test-context-3")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        
        taskStore.save(initialTask);

        // Update the task
        Task updatedTask = new Task.Builder()
                .id("test-task-3")
                .contextId("test-context-3")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();
        
        taskStore.save(updatedTask);

        // Retrieve and verify the update
        Task retrieved = taskStore.get("test-task-3");
        
        assertNotNull(retrieved);
        assertEquals("test-task-3", retrieved.getId());
        assertEquals(TaskState.COMPLETED, retrieved.getStatus().state());
    }

    @Test
    @Transactional
    public void testGetNonExistentTask() {
        Task retrieved = taskStore.get("non-existent-task");
        assertNull(retrieved);
    }

    @Test
    @Transactional
    public void testDeleteTask() {
        // Create and save a task
        Task task = new Task.Builder()
                .id("test-task-4")
                .contextId("test-context-4")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        
        taskStore.save(task);

        // Verify it exists
        assertNotNull(taskStore.get("test-task-4"));

        // Delete the task
        taskStore.delete("test-task-4");

        // Verify it's gone
        assertNull(taskStore.get("test-task-4"));
    }

    @Test
    @Transactional
    public void testDeleteNonExistentTask() {
        // This should not throw an exception
        taskStore.delete("non-existent-task");
    }

    @Test
    @Transactional
    public void testTaskWithComplexMetadata() {
        // Create a task with complex metadata
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 42);
        metadata.put("key3", true);
        
        Task task = new Task.Builder()
                .id("test-task-5")
                .contextId("test-context-5")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .metadata(metadata)
                .build();

        // Save and retrieve
        taskStore.save(task);
        Task retrieved = taskStore.get("test-task-5");
        
        assertNotNull(retrieved);
        assertEquals("test-task-5", retrieved.getId());
        assertNotNull(retrieved.getMetadata());
        assertEquals("value1", retrieved.getMetadata().get("key1"));
        assertEquals(42, retrieved.getMetadata().get("key2"));
        assertEquals(true, retrieved.getMetadata().get("key3"));
    }

    @Test
    @Transactional
    public void testIsTaskActiveForNonFinalTask() {
        // Create a task in non-final state
        Task task = new Task.Builder()
                .id("test-task-active-1")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.WORKING))
                .build();
        
        taskStore.save(task);
        
        // Task should be active (not in final state)
        JpaDatabaseTaskStore jpaDatabaseTaskStore = (JpaDatabaseTaskStore) taskStore;
        boolean isActive = jpaDatabaseTaskStore.isTaskActive("test-task-active-1");
        
        assertEquals(true, isActive, "Non-final task should be active");
    }

    @Test
    @Transactional
    public void testIsTaskActiveForFinalTaskWithinGracePeriod() {
        // Create a task and update it to final state
        Task task = new Task.Builder()
                .id("test-task-active-2")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.WORKING))
                .build();
        
        taskStore.save(task);
        
        // Update to final state
        Task finalTask = new Task.Builder()
                .id("test-task-active-2")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();
        
        taskStore.save(finalTask);
        
        // Task should be active (within grace period - default 15 seconds)
        JpaDatabaseTaskStore jpaDatabaseTaskStore = (JpaDatabaseTaskStore) taskStore;
        boolean isActive = jpaDatabaseTaskStore.isTaskActive("test-task-active-2");
        
        assertEquals(true, isActive, "Final task within grace period should be active");
    }

    @Test
    @Transactional
    public void testIsTaskActiveForFinalTaskBeyondGracePeriod() {
        // Create and save a task in final state
        Task task = new Task.Builder()
                .id("test-task-active-3")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();
        
        taskStore.save(task);
        
        // Directly update the finalizedAt timestamp to 20 seconds in the past
        // (beyond the default 15-second grace period)
        JpaTask jpaTask = entityManager.find(JpaTask.class, "test-task-active-3");
        assertNotNull(jpaTask);
        
        // Manually set finalizedAt to 20 seconds in the past
        java.time.Instant pastTime = java.time.Instant.now().minusSeconds(20);
        entityManager.createQuery("UPDATE JpaTask j SET j.finalizedAt = :finalizedAt WHERE j.id = :id")
                .setParameter("finalizedAt", pastTime)
                .setParameter("id", "test-task-active-3")
                .executeUpdate();
        
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force fresh read
        
        // Task should be inactive (beyond grace period)
        JpaDatabaseTaskStore jpaDatabaseTaskStore = (JpaDatabaseTaskStore) taskStore;
        boolean isActive = jpaDatabaseTaskStore.isTaskActive("test-task-active-3");
        
        assertEquals(false, isActive, "Final task beyond grace period should be inactive");
    }

    @Test
    @Transactional
    public void testIsTaskActiveForNonExistentTask() {
        JpaDatabaseTaskStore jpaDatabaseTaskStore = (JpaDatabaseTaskStore) taskStore;
        boolean isActive = jpaDatabaseTaskStore.isTaskActive("non-existent-task");
        
        assertEquals(false, isActive, "Non-existent task should be inactive");
    }
}
