package io.a2a.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.Artifact;
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.ListTasksResult;
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
        Task task = Task.builder()
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
        Message message = Message.builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("Hello, agent!")))
                .messageId("msg-1")
                .build();

        // Create a task with history
        Task task = Task.builder()
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
        Task initialTask = Task.builder()
                .id("test-task-3")
                .contextId("test-context-3")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        
        taskStore.save(initialTask);

        // Update the task
        Task updatedTask = Task.builder()
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
        Task task = Task.builder()
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
        
        Task task = Task.builder()
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
        assertEquals(42, ((Number)retrieved.getMetadata().get("key2")).intValue());
        assertEquals(true, retrieved.getMetadata().get("key3"));
    }

    @Test
    @Transactional
    public void testIsTaskActiveForNonFinalTask() {
        // Create a task in non-final state
        Task task = Task.builder()
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
        Task task = Task.builder()
                .id("test-task-active-2")
                .contextId("test-context")
                .status(new TaskStatus(TaskState.WORKING))
                .build();
        
        taskStore.save(task);
        
        // Update to final state
        Task finalTask = Task.builder()
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
        Task task = Task.builder()
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

    // ===== list() method tests =====

    @Test
    @Transactional
    public void testListTasksEmpty() {
        // List with specific context that has no tasks
        ListTasksParams params = ListTasksParams.builder()
                .contextId("non-existent-context-12345")
                .tenant("tenant")
                .build();
        ListTasksResult result = taskStore.list(params);

        assertNotNull(result);
        assertEquals(0, result.totalSize());
        assertEquals(0, result.pageSize());
        assertTrue(result.tasks().isEmpty());
        assertNull(result.nextPageToken());
    }

    @Test
    @Transactional
    public void testListTasksFilterByContextId() {
        // Create tasks with different context IDs
        Task task1 = Task.builder()
                .id("task-context-1")
                .contextId("context-A")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        Task task2 = Task.builder()
                .id("task-context-2")
                .contextId("context-A")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        Task task3 = Task.builder()
                .id("task-context-3")
                .contextId("context-B")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();

        taskStore.save(task1);
        taskStore.save(task2);
        taskStore.save(task3);

        // List tasks for context-A
        ListTasksParams params = ListTasksParams.builder()
                .contextId("context-A")
                .tenant("tenant")
                .build();
        ListTasksResult result = taskStore.list(params);

        assertEquals(2, result.totalSize());
        assertEquals(2, result.pageSize());
        assertEquals(2, result.tasks().size());
        assertTrue(result.tasks().stream().allMatch(t -> "context-A".equals(t.getContextId())));
    }

    @Test
    @Transactional
    public void testListTasksFilterByStatus() {
        // Create tasks with different statuses - use unique context
        Task task1 = Task.builder()
                .id("task-status-filter-1")
                .contextId("context-status-filter-test")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        Task task2 = Task.builder()
                .id("task-status-filter-2")
                .contextId("context-status-filter-test")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        Task task3 = Task.builder()
                .id("task-status-filter-3")
                .contextId("context-status-filter-test")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();

        taskStore.save(task1);
        taskStore.save(task2);
        taskStore.save(task3);

        // List only WORKING tasks in this context
        ListTasksParams params = ListTasksParams.builder()
                .contextId("context-status-filter-test")
                .tenant("tenant")
                .status(TaskState.WORKING)
                .build();
        ListTasksResult result = taskStore.list(params);

        assertEquals(1, result.totalSize());
        assertEquals(1, result.pageSize());
        assertEquals(1, result.tasks().size());
        assertEquals(TaskState.WORKING, result.tasks().get(0).getStatus().state());
    }

    @Test
    @Transactional
    public void testListTasksCombinedFilters() {
        // Create tasks with various context IDs and statuses
        Task task1 = Task.builder()
                .id("task-combined-1")
                .contextId("context-X")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        Task task2 = Task.builder()
                .id("task-combined-2")
                .contextId("context-X")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        Task task3 = Task.builder()
                .id("task-combined-3")
                .contextId("context-Y")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        taskStore.save(task1);
        taskStore.save(task2);
        taskStore.save(task3);

        // List WORKING tasks in context-X
        ListTasksParams params = ListTasksParams.builder()
                .contextId("context-X")
                .tenant("tenant")
                .status(TaskState.WORKING)
                .build();
        ListTasksResult result = taskStore.list(params);

        assertEquals(1, result.totalSize());
        assertEquals(1, result.pageSize());
        assertEquals("task-combined-2", result.tasks().get(0).getId());
        assertEquals("context-X", result.tasks().get(0).getContextId());
        assertEquals(TaskState.WORKING, result.tasks().get(0).getStatus().state());
    }

    @Test
    @Transactional
    public void testListTasksPagination() {
        // Create 5 tasks with same timestamp to ensure ID-based pagination works
        // (With timestamp DESC sorting, same timestamps allow ID ASC tie-breaking)
        OffsetDateTime sameTimestamp = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        for (int i = 1; i <= 5; i++) {
            Task task = Task.builder()
                    .id("task-page-" + i)
                    .contextId("context-pagination")
                    .status(new TaskStatus(TaskState.SUBMITTED, null, sameTimestamp))
                    .build();
            taskStore.save(task);
        }

        // First page: pageSize=2
        ListTasksParams params1 = ListTasksParams.builder()
                .contextId("context-pagination")
                .tenant("tenant")
                .pageSize(2)
                .build();
        ListTasksResult result1 = taskStore.list(params1);

        assertEquals(5, result1.totalSize());
        assertEquals(2, result1.pageSize());
        assertEquals(2, result1.tasks().size());
        assertNotNull(result1.nextPageToken(), "Should have next page token");

        // Second page: use pageToken from first page
        ListTasksParams params2 = ListTasksParams.builder()
                .contextId("context-pagination")
                .tenant("tenant")
                .pageSize(2)
                .pageToken(result1.nextPageToken())
                .build();
        ListTasksResult result2 = taskStore.list(params2);

        assertEquals(5, result2.totalSize());
        assertEquals(2, result2.pageSize());
        assertNotNull(result2.nextPageToken(), "Should have next page token");

        // Third page: last page
        ListTasksParams params3 = ListTasksParams.builder()
                .contextId("context-pagination")
                .tenant("tenant")
                .pageSize(2)
                .pageToken(result2.nextPageToken())
                .build();
        ListTasksResult result3 = taskStore.list(params3);

        assertEquals(5, result3.totalSize());
        assertEquals(1, result3.pageSize());
        assertNull(result3.nextPageToken(), "Last page should have no next page token");
    }

    @Test
    @Transactional
    public void testListTasksPaginationWithDifferentTimestamps() {
        // Create tasks with different timestamps to verify keyset pagination
        // with composite sort (timestamp DESC, id ASC)
        OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);

        // Task 1: 10 minutes ago, ID="task-diff-a"
        Task task1 = Task.builder()
                .id("task-diff-a")
                .contextId("context-diff-timestamps")
                .status(new TaskStatus(TaskState.WORKING, null, now.minusMinutes(10)))
                .build();
        taskStore.save(task1);

        // Task 2: 5 minutes ago, ID="task-diff-b"
        Task task2 = Task.builder()
                .id("task-diff-b")
                .contextId("context-diff-timestamps")
                .status(new TaskStatus(TaskState.WORKING, null, now.minusMinutes(5)))
                .build();
        taskStore.save(task2);

        // Task 3: 5 minutes ago, ID="task-diff-c" (same timestamp as task2, tests ID tie-breaker)
        Task task3 = Task.builder()
                .id("task-diff-c")
                .contextId("context-diff-timestamps")
                .status(new TaskStatus(TaskState.WORKING, null, now.minusMinutes(5)))
                .build();
        taskStore.save(task3);

        // Task 4: Now, ID="task-diff-d"
        Task task4 = Task.builder()
                .id("task-diff-d")
                .contextId("context-diff-timestamps")
                .status(new TaskStatus(TaskState.WORKING, null, now))
                .build();
        taskStore.save(task4);

        // Task 5: 1 minute ago, ID="task-diff-e"
        Task task5 = Task.builder()
                .id("task-diff-e")
                .contextId("context-diff-timestamps")
                .status(new TaskStatus(TaskState.WORKING, null, now.minusMinutes(1)))
                .build();
        taskStore.save(task5);

        // Expected order (timestamp DESC, id ASC):
        // 1. task-diff-d (now)
        // 2. task-diff-e (1 min ago)
        // 3. task-diff-b (5 min ago, ID 'b')
        // 4. task-diff-c (5 min ago, ID 'c')
        // 5. task-diff-a (10 min ago)

        // Page 1: Get first 2 tasks
        ListTasksParams params1 = ListTasksParams.builder()
                .contextId("context-diff-timestamps")
                .tenant("tenant")
                .pageSize(2)
                .build();
        ListTasksResult result1 = taskStore.list(params1);

        assertEquals(5, result1.totalSize());
        assertEquals(2, result1.pageSize());
        assertNotNull(result1.nextPageToken(), "Should have next page token");

        // Verify first page order
        assertEquals("task-diff-d", result1.tasks().get(0).getId(), "First task should be most recent");
        assertEquals("task-diff-e", result1.tasks().get(1).getId(), "Second task should be 1 min ago");

        // Verify pageToken format: "timestamp_millis:taskId"
        assertTrue(result1.nextPageToken().contains(":"), "PageToken should have format timestamp:id");
        String[] tokenParts = result1.nextPageToken().split(":", 2);
        assertEquals(2, tokenParts.length, "PageToken should have exactly 2 parts");
        assertEquals("task-diff-e", tokenParts[1], "PageToken should contain last task ID");

        // Page 2: Get next 2 tasks
        ListTasksParams params2 = ListTasksParams.builder()
                .contextId("context-diff-timestamps")
                .tenant("tenant")
                .pageSize(2)
                .pageToken(result1.nextPageToken())
                .build();
        ListTasksResult result2 = taskStore.list(params2);

        assertEquals(5, result2.totalSize());
        assertEquals(2, result2.pageSize());
        assertNotNull(result2.nextPageToken(), "Should have next page token");

        // Verify second page order (tasks with same timestamp, sorted by ID)
        assertEquals("task-diff-b", result2.tasks().get(0).getId(), "Third task should be 5 min ago, ID 'b'");
        assertEquals("task-diff-c", result2.tasks().get(1).getId(), "Fourth task should be 5 min ago, ID 'c'");

        // Page 3: Get last task
        ListTasksParams params3 = ListTasksParams.builder()
                .contextId("context-diff-timestamps")
                .tenant("tenant")
                .pageSize(2)
                .pageToken(result2.nextPageToken())
                .build();
        ListTasksResult result3 = taskStore.list(params3);

        assertEquals(5, result3.totalSize());
        assertEquals(1, result3.pageSize());
        assertNull(result3.nextPageToken(), "Last page should have no next page token");

        // Verify last task
        assertEquals("task-diff-a", result3.tasks().get(0).getId(), "Last task should be oldest");

        // Verify no duplicates across all pages
        List<String> allTaskIds = new ArrayList<>();
        allTaskIds.addAll(result1.tasks().stream().map(Task::getId).toList());
        allTaskIds.addAll(result2.tasks().stream().map(Task::getId).toList());
        allTaskIds.addAll(result3.tasks().stream().map(Task::getId).toList());

        assertEquals(5, allTaskIds.size(), "Should have exactly 5 tasks across all pages");
        assertEquals(5, allTaskIds.stream().distinct().count(), "Should have no duplicate tasks");
    }

    @Test
    @Transactional
    public void testListTasksHistoryLimiting() {
        // Create messages for history
        List<Message> longHistory = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Message message = Message.builder()
                    .role(Message.Role.USER)
                    .parts(Collections.singletonList(new TextPart("Message " + i)))
                    .messageId("msg-history-limit-" + i)
                    .build();
            longHistory.add(message);
        }

        // Create task with long history - use unique context
        Task task = Task.builder()
                .id("task-history-limit-unique-1")
                .contextId("context-history-limit-unique")
                .status(new TaskStatus(TaskState.WORKING))
                .history(longHistory)
                .build();

        taskStore.save(task);

        // List with historyLength=3 (should keep only last 3 messages) - filter by unique context
        ListTasksParams params = ListTasksParams.builder()
                .contextId("context-history-limit-unique")
                .tenant("tenant")
                .historyLength(3)
                .build();
        ListTasksResult result = taskStore.list(params);

        assertEquals(1, result.tasks().size());
        Task retrieved = result.tasks().get(0);
        assertEquals(3, retrieved.getHistory().size());
        // Should have messages 8, 9, 10 (last 3)
        assertEquals("msg-history-limit-8", retrieved.getHistory().get(0).getMessageId());
        assertEquals("msg-history-limit-9", retrieved.getHistory().get(1).getMessageId());
        assertEquals("msg-history-limit-10", retrieved.getHistory().get(2).getMessageId());
    }

    @Test
    @Transactional
    public void testListTasksArtifactInclusion() {
        // Create task with artifacts - use unique context
        List<Artifact> artifacts = new ArrayList<>();
        Artifact artifact = Artifact.builder()
                .artifactId("artifact-unique-1")
                .name("test-artifact")
                .parts(Collections.singletonList(new TextPart("Artifact content")))
                .build();
        artifacts.add(artifact);

        Task task = Task.builder()
                .id("task-artifact-unique-1")
                .contextId("context-artifact-unique")
                .status(new TaskStatus(TaskState.COMPLETED))
                .artifacts(artifacts)
                .build();

        taskStore.save(task);

        // List without artifacts (default) - filter by unique context
        ListTasksParams paramsWithoutArtifacts = ListTasksParams.builder()
                .contextId("context-artifact-unique")
                .tenant("tenant")
                .build();
        ListTasksResult resultWithout = taskStore.list(paramsWithoutArtifacts);

        assertEquals(1, resultWithout.tasks().size());
        assertTrue(resultWithout.tasks().get(0).getArtifacts().isEmpty(),
                "By default, artifacts should be excluded");

        // List with artifacts - filter by unique context
        ListTasksParams paramsWithArtifacts = ListTasksParams.builder()
                .contextId("context-artifact-unique")
                .tenant("tenant")
                .includeArtifacts(true)
                .build();
        ListTasksResult resultWith = taskStore.list(paramsWithArtifacts);

        assertEquals(1, resultWith.tasks().size());
        assertEquals(1, resultWith.tasks().get(0).getArtifacts().size(),
                "When includeArtifacts=true, artifacts should be included");
        assertEquals("artifact-unique-1", resultWith.tasks().get(0).getArtifacts().get(0).artifactId());
    }

    @Test
    @Transactional
    public void testListTasksDefaultPageSize() {
        // Create 100 tasks (more than default page size of 50)
        for (int i = 1; i <= 100; i++) {
            Task task = Task.builder()
                    .id("task-default-pagesize-" + String.format("%03d", i))
                    .contextId("context-default-pagesize")
                    .status(new TaskStatus(TaskState.SUBMITTED))
                    .build();
            taskStore.save(task);
        }

        // List without specifying pageSize (should use default of 50)
        ListTasksParams params = ListTasksParams.builder()
                .contextId("context-default-pagesize")
                .tenant("tenant")
                .build();
        ListTasksResult result = taskStore.list(params);

        assertEquals(100, result.totalSize());
        assertEquals(50, result.pageSize(), "Default page size should be 50");
        assertNotNull(result.nextPageToken(), "Should have next page");
    }

    @Test
    @Transactional
    public void testListTasksInvalidPageTokenFormat() {
        // Create a task
        Task task = Task.builder()
                .id("task-invalid-token")
                .contextId("context-invalid-token")
                .status(new TaskStatus(TaskState.WORKING))
                .build();
        taskStore.save(task);

        // Test 1: Legacy ID-only pageToken should throw InvalidParamsError
        ListTasksParams params1 = ListTasksParams.builder()
                .contextId("context-invalid-token")
                .tenant("tenant")
                .pageToken("task-invalid-token")  // ID-only format (legacy)
                .build();

        try {
            taskStore.list(params1);
            throw new AssertionError("Expected InvalidParamsError for legacy ID-only pageToken");
        } catch (io.a2a.spec.InvalidParamsError e) {
            // Expected - legacy format not supported
            assertTrue(e.getMessage().contains("Invalid pageToken format"),
                    "Error message should mention invalid format");
        }

        // Test 2: Malformed timestamp in pageToken should throw InvalidParamsError
        ListTasksParams params2 = ListTasksParams.builder()
                .contextId("context-invalid-token")
                .tenant("tenant")
                .pageToken("not-a-number:task-id")  // Invalid timestamp
                .build();

        try {
            taskStore.list(params2);
            throw new AssertionError("Expected InvalidParamsError for malformed timestamp");
        } catch (io.a2a.spec.InvalidParamsError e) {
            // Expected - malformed timestamp
            assertTrue(e.getMessage().contains("timestamp must be numeric"),
                    "Error message should mention numeric timestamp requirement");
        }
    }


    @Test
    @Transactional
    public void testListTasksOrderingById() {
        // Create tasks with same timestamp to test ID-based tie-breaking
        // (spec requires sorting by timestamp DESC, then ID ASC)
        OffsetDateTime sameTimestamp = OffsetDateTime.now(java.time.ZoneOffset.UTC);

        Task task1 = Task.builder()
                .id("task-order-a")
                .contextId("context-order")
                .status(new TaskStatus(TaskState.SUBMITTED, null, sameTimestamp))
                .build();

        Task task2 = Task.builder()
                .id("task-order-b")
                .contextId("context-order")
                .status(new TaskStatus(TaskState.SUBMITTED, null, sameTimestamp))
                .build();

        Task task3 = Task.builder()
                .id("task-order-c")
                .contextId("context-order")
                .status(new TaskStatus(TaskState.SUBMITTED, null, sameTimestamp))
                .build();

        // Save in reverse order
        taskStore.save(task3);
        taskStore.save(task1);
        taskStore.save(task2);

        // List should return sorted by timestamp DESC (all same), then by ID ASC
        ListTasksParams params = ListTasksParams.builder()
                .contextId("context-order")
                .tenant("tenant")
                .build();
        ListTasksResult result = taskStore.list(params);

        assertEquals(3, result.tasks().size());
        assertEquals("task-order-a", result.tasks().get(0).getId());
        assertEquals("task-order-b", result.tasks().get(1).getId());
        assertEquals("task-order-c", result.tasks().get(2).getId());
    }
}
