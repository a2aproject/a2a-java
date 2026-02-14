package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.a2a.client.Client;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.Message;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TextPart;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration test that verifies the JPA PushNotificationConfigStore works correctly
 * with the full client-server flow using the Client API.
 */
@QuarkusTest
public class JpaDatabasePushNotificationConfigStoreIntegrationTest {

    @Inject
    PushNotificationConfigStore pushNotificationConfigStore;

    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    @Inject
    MockPushNotificationSender mockPushNotificationSender;

    private Client client;

    @BeforeEach
    public void setup() throws A2AClientException {
        // Clear any previous notifications
        mockPushNotificationSender.clear();

        // Create client configuration - enable streaming for automatic push notifications
        ClientConfig clientConfig = new ClientConfig.Builder()
            .setStreaming(true)
            .build();

        // Build client with JSON-RPC transport
        client = Client.builder(agentCard)
            .clientConfig(clientConfig)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
            .build();
    }

    @Test
    public void testIsJpaDatabasePushNotificationConfigStore() {
        assertInstanceOf(JpaDatabasePushNotificationConfigStore.class, pushNotificationConfigStore);
    }

    @Test
    public void testDirectNotificationTrigger() {
        // Simple test to verify the mock notification mechanism works
        mockPushNotificationSender.clear();

        Task testTask = Task.builder()
            .id("direct-test-task")
            .contextId("test-context")
            .status(new io.a2a.spec.TaskStatus(io.a2a.spec.TaskState.SUBMITTED))
            .build();

        // Directly trigger the mock
        mockPushNotificationSender.sendNotification(testTask);

        // Verify it was captured
        Queue<Task> captured = mockPushNotificationSender.getCapturedTasks();
        assertEquals(1, captured.size());
        assertEquals("direct-test-task", captured.peek().id());
    }

    @Test
    public void testJpaDatabasePushNotificationConfigStoreIntegration() throws Exception {
        final String taskId = "push-notify-test-" + System.currentTimeMillis();
        final String contextId = "test-context";

        // Step 1: Create the task
        Message createMessage = Message.builder()
            .role(Message.Role.USER)
            .parts(List.of(new TextPart("create"))) // Send the "create" command
            .taskId(taskId)
            .messageId("test-msg-1")
            .contextId(contextId)
            .build();

        // Use a latch to wait for the first operation to complete
        CountDownLatch createLatch = new CountDownLatch(1);
        client.sendMessage(createMessage, List.of((event, card) -> createLatch.countDown()), (e) -> createLatch.countDown());
        assertTrue(createLatch.await(10, TimeUnit.SECONDS), "Timeout waiting for task creation");

        // Step 2: Set the push notification configuration
        PushNotificationConfig pushConfig = PushNotificationConfig.builder()
            .url("http://localhost:9999/mock-endpoint")
            .token("test-token-123")
            .id("test-config-1")
            .build();

        TaskPushNotificationConfig taskPushConfig = new TaskPushNotificationConfig(taskId, pushConfig, "");
        TaskPushNotificationConfig setResult = client.createTaskPushNotificationConfiguration(taskPushConfig);
        assertNotNull(setResult);

        // Step 3: Verify the configuration was stored using client API
        TaskPushNotificationConfig storedConfig = client.getTaskPushNotificationConfiguration(
            new GetTaskPushNotificationConfigParams(taskId, "test-config-1"));

        assertNotNull(storedConfig);
        assertEquals(taskId, storedConfig.taskId());
        assertEquals("test-config-1", storedConfig.config().id());
        assertEquals("http://localhost:9999/mock-endpoint", storedConfig.config().url());
        assertEquals("test-token-123", storedConfig.config().token());

        // Step 4: Update the task to trigger the notification
        Message updateMessage = Message.builder()
            .role(Message.Role.USER)
            .parts(List.of(new TextPart("update"))) // Send the "update" command
            .taskId(taskId)
            .messageId("test-msg-2")
            .contextId(contextId)
            .build();

        CountDownLatch updateLatch = new CountDownLatch(1);
        client.sendMessage(updateMessage, List.of((event, card) -> updateLatch.countDown()), (e) -> updateLatch.countDown());
        assertTrue(updateLatch.await(10, TimeUnit.SECONDS), "Timeout waiting for task update");

        // Step 5: Poll for the async notification to be captured
        // With the new StreamingEventKind support, we receive all event types (Task, Message, TaskArtifactUpdateEvent, etc.)
        long end = System.currentTimeMillis() + 5000;
        boolean notificationReceived = false;

        while (System.currentTimeMillis() < end) {
            if (!mockPushNotificationSender.getCapturedEvents().isEmpty()) {
                notificationReceived = true;
                break;
            }
            Thread.sleep(100);
        }

        assertTrue(notificationReceived, "Timeout waiting for push notification.");

        // Step 6: Verify the captured notification
        // Check if we received events for this task (could be Task, TaskArtifactUpdateEvent, etc.)
        Queue<io.a2a.spec.StreamingEventKind> capturedEvents = mockPushNotificationSender.getCapturedEvents();

        // Look for Task events with artifacts OR TaskArtifactUpdateEvent for this task
        boolean hasTaskWithArtifact = capturedEvents.stream()
            .filter(e -> e instanceof Task)
            .map(e -> (Task) e)
            .anyMatch(t -> taskId.equals(t.id()) && t.artifacts() != null && t.artifacts().size() > 0);

        boolean hasArtifactUpdateEvent = capturedEvents.stream()
            .filter(e -> e instanceof io.a2a.spec.TaskArtifactUpdateEvent)
            .map(e -> (io.a2a.spec.TaskArtifactUpdateEvent) e)
            .anyMatch(e -> taskId.equals(e.taskId()));

        assertTrue(hasTaskWithArtifact || hasArtifactUpdateEvent,
            "Notification should contain either Task with artifacts or TaskArtifactUpdateEvent for task " + taskId);

        // Step 7: Clean up - delete the push notification configuration
        client.deleteTaskPushNotificationConfigurations(
            new DeleteTaskPushNotificationConfigParams(taskId, "test-config-1"));

        // Verify deletion by asserting that getting the config now throws an exception
        assertThrows(A2AClientException.class, () -> {
            client.getTaskPushNotificationConfiguration(new GetTaskPushNotificationConfigParams(taskId, "test-config-1"));
        }, "Getting a deleted config should throw an A2AClientException");
    }

    private PushNotificationConfig createSamplePushConfig(String url, String configId, String token) {
        return PushNotificationConfig.builder()
                .url(url)
                .id(configId)
                .token(token)
                .build();
    }

    @Test
    @Transactional
    public void testPaginationWithPageSize() {
        String taskId = "task_pagination_" + System.currentTimeMillis();
        // Create 5 configs
        createSamples(taskId, 5);
        // Request first page with pageSize=2
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

        assertNotNull(result);
        assertEquals(2, result.configs().size(), "Should return 2 configs");
        assertNotNull(result.nextPageToken(), "Should have nextPageToken when more items exist");
    }

    @Test
    @Transactional
    public void testPaginationWithPageToken() {
        String taskId = "task_pagination_token_" + System.currentTimeMillis();
        // Create 5 configs
        createSamples(taskId, 5);

        // Get first page
        ListTaskPushNotificationConfigParams firstPageParams = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult firstPage = pushNotificationConfigStore.getInfo(firstPageParams);
        assertNotNull(firstPage.nextPageToken());

        // Get second page using nextPageToken
        ListTaskPushNotificationConfigParams secondPageParams = new ListTaskPushNotificationConfigParams(
                taskId, 2, firstPage.nextPageToken(), "");
        ListTaskPushNotificationConfigResult secondPage = pushNotificationConfigStore.getInfo(secondPageParams);

        assertNotNull(secondPage);
        assertEquals(2, secondPage.configs().size(), "Should return 2 configs for second page");
        assertNotNull(secondPage.nextPageToken(), "Should have nextPageToken when more items exist");

        // Verify NO overlap between pages - collect all IDs from both pages
        List<String> firstPageIds = firstPage.configs().stream()
                .map(c -> c.config().id())
                .toList();
        List<String> secondPageIds = secondPage.configs().stream()
                .map(c -> c.config().id())
                .toList();

        // Check that no ID from first page appears in second page
        for (String id : firstPageIds) {
            assertTrue(!secondPageIds.contains(id),
                "Config " + id + " appears in both pages - overlap detected!");
        }

        // Also verify the pages are sequential (first page ends before second page starts)
        // Since configs are created in order, we can verify the IDs.
        // There is no spec about pagination for PushNotifications, hence following the Task List
        // behavior by which recent notifications are returned first
        assertEquals("cfg4", firstPageIds.get(0));
        assertEquals("cfg3", firstPageIds.get(1));
        assertEquals("cfg2", secondPageIds.get(0));
        assertEquals("cfg1", secondPageIds.get(1));
    }

    @Test
    @Transactional
    public void testPaginationLastPage() {
        String taskId = "task_pagination_last_" + System.currentTimeMillis();
        // Create 5 configs
        createSamples(taskId, 5);

        // Get first page (2 items)
        ListTaskPushNotificationConfigParams firstPageParams = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult firstPage = pushNotificationConfigStore.getInfo(firstPageParams);

        // Get second page (2 items)
        ListTaskPushNotificationConfigParams secondPageParams = new ListTaskPushNotificationConfigParams(
                taskId, 2, firstPage.nextPageToken(), "");
        ListTaskPushNotificationConfigResult secondPage = pushNotificationConfigStore.getInfo(secondPageParams);

        // Get last page (1 item remaining)
        ListTaskPushNotificationConfigParams lastPageParams = new ListTaskPushNotificationConfigParams(
                taskId, 2, secondPage.nextPageToken(), "");
        ListTaskPushNotificationConfigResult lastPage = pushNotificationConfigStore.getInfo(lastPageParams);

        assertNotNull(lastPage);
        assertEquals(1, lastPage.configs().size(), "Last page should have 1 remaining config");
        assertNull(lastPage.nextPageToken(), "Last page should not have nextPageToken");
    }

    @Test
    @Transactional
    public void testPaginationWithZeroPageSize() {
        String taskId = "task_pagination_zero_" + System.currentTimeMillis();
        // Create 5 configs
        createSamples(taskId, 5);

        // Request with pageSize=0 should return all configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 0, "", "");
        ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

        assertNotNull(result);
        assertEquals(5, result.configs().size(), "Should return all 5 configs when pageSize=0");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when returning all");
    }

    @Test
    @Transactional
    public void testPaginationWithNegativePageSize() {
        String taskId = "task_pagination_negative_" + System.currentTimeMillis();
        // Create 3 configs
        createSamples(taskId, 3);

        // Request with negative pageSize should return all configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, -1, "", "");
        ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

        assertNotNull(result);
        assertEquals(3, result.configs().size(), "Should return all configs when pageSize is negative");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when returning all");
    }

    @Test
    @Transactional
    public void testPaginationPageSizeLargerThanConfigs() {
        String taskId = "task_pagination_large_" + System.currentTimeMillis();
        // Create 3 configs
        createSamples(taskId, 3);

        // Request with pageSize larger than available configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 10, "", "");
        ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

        assertNotNull(result);
        assertEquals(3, result.configs().size(), "Should return all 3 configs");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when all configs fit in one page");
    }

    @Test
    @Transactional
    public void testPaginationExactlyPageSize() {
        String taskId = "task_pagination_exact_" + System.currentTimeMillis();
        // Create exactly 3 configs
        createSamples(taskId, 3);

        // Request with pageSize equal to number of configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 3, "", "");
        ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

        assertNotNull(result);
        assertEquals(3, result.configs().size(), "Should return all 3 configs");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when configs exactly match pageSize");
    }

    @Test
    @Transactional
    public void testPaginationWithInvalidToken() {
        String taskId = "task_pagination_invalid_token_" + System.currentTimeMillis();
        // Create 5 configs
        createSamples(taskId, 5);

        // Request with invalid pageToken - should throw InvalidParamsError for invalid format
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(
                taskId, 2, "invalid_token_that_does_not_exist", "");

        assertThrows(io.a2a.spec.InvalidParamsError.class, () -> pushNotificationConfigStore.getInfo(params),
          "Should throw InvalidParamsError for invalid pageToken format (missing colon)");
    }

    @Test
    @Transactional
    public void testPaginationEmptyTaskWithPageSize() {
        String taskId = "task_pagination_empty_" + System.currentTimeMillis();
        // No configs created

        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

        assertNotNull(result);
        assertTrue(result.configs().isEmpty(), "Should return empty list for non-existent task");
        assertNull(result.nextPageToken(), "Should not have nextPageToken for empty result");
    }

    @Test
    @Transactional
    public void testPaginationFullIteration() {
        String taskId = "task_pagination_full_" + System.currentTimeMillis();
        // Create 7 configs
        createSamples(taskId, 7);

        // Iterate through all pages with pageSize=3
        int totalCollected = 0;
        String pageToken = "";
        int pageCount = 0;

        do {
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 3, pageToken, "");
            ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

            totalCollected += result.configs().size();
            pageToken = result.nextPageToken();
            pageCount++;

            // Safety check to prevent infinite loop
            assertTrue(pageCount <= 7, "Should not have more than 7 pages for 7 configs");

        } while (pageToken != null);

        assertEquals(7, totalCollected, "Should collect all 7 configs across all pages");
        assertEquals(3, pageCount, "Should have exactly 3 pages (3+3+1)");
    }

    @Test
    @Transactional
    public void testPageTokenWithNonNumericTimestamp() {
      String taskId = "task_malformed_token_" + System.currentTimeMillis();
      createSamples(taskId, 3);

      ListTaskPushNotificationConfigParams params =
          new ListTaskPushNotificationConfigParams(taskId, 2, "not_a_number:cfg1", "");

      assertThrows(io.a2a.spec.InvalidParamsError.class,
          () -> pushNotificationConfigStore.getInfo(params),
          "Should throw InvalidParamsError for non-numeric timestamp in pageToken");
    }

    @Test
    @Transactional
    public void testPageTokenWithMissingColon() {
      String taskId = "task_missing_colon_" + System.currentTimeMillis();
      createSamples(taskId, 5);

      ListTaskPushNotificationConfigParams params =
          new ListTaskPushNotificationConfigParams(taskId, 2, "123456789cfg1", "");

      assertThrows(io.a2a.spec.InvalidParamsError.class, () -> pushNotificationConfigStore.getInfo(params),
          "Should throw InvalidParamsError for invalid pageToken format (missing colon)");
    }

    @Test
    @Transactional
    public void testPaginationBoundaryExactlyMaxResultsPlusOne() {
      String taskId = "task_boundary_" + System.currentTimeMillis();

      createSamples(taskId, 4);

      ListTaskPushNotificationConfigParams params =
          new ListTaskPushNotificationConfigParams(taskId, 4, "", "");
      ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

      assertEquals(4, result.configs().size(),
          "Should return all 4 configs when pageSize equals total count");
      assertNull(result.nextPageToken(),
          "Should not have nextPageToken when count equals pageSize");
    }

    @Test
    @Transactional
    public void testMultipleTasksDoNotInterfere() {
      String taskId1 = "task1_" + System.currentTimeMillis();
      String taskId2 = "task2_" + System.currentTimeMillis();

      createSamples(taskId1, 3);
      createSamples(taskId2, 2);

      ListTaskPushNotificationConfigResult result1 =
          pushNotificationConfigStore.getInfo(new ListTaskPushNotificationConfigParams(taskId1));
      ListTaskPushNotificationConfigResult result2 =
          pushNotificationConfigStore.getInfo(new ListTaskPushNotificationConfigParams(taskId2));

      assertEquals(3, result1.configs().size(), "Task1 should have 3 configs");
      assertEquals(2, result2.configs().size(), "Task2 should have 2 configs");

      List<String> task1Ids = result1.configs().stream()
          .map(c -> taskId1 + c.config().id())
          .toList();
      List<String> task2Ids = result2.configs().stream()
          .map(c -> taskId2 + c.config().id())
          .toList();

      for (String id : task1Ids) {
        assertTrue(!task2Ids.contains(id),
            "Configs from different tasks should not overlap");
      }
    }

    @Test
    @Transactional
    public void testGetInfoWithNonExistentTaskIdDoesNotThrow() {
      String nonExistentTaskId = "non_existent_task_" + System.currentTimeMillis();

      ListTaskPushNotificationConfigParams params =
          new ListTaskPushNotificationConfigParams(nonExistentTaskId, 10, "", "");
      ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

      assertNotNull(result, "Result should not be null");
      assertTrue(result.configs().isEmpty(),
          "Should return empty list for non-existent task");
      assertNull(result.nextPageToken(),
          "Should not have nextPageToken for empty result");
    }

    @Test
    @Transactional
    public void testGetInfoReturnsTenantFromParams() {
      String taskId = "task_tenant_" + System.currentTimeMillis();
      String tenant = "test-tenant-123";

      PushNotificationConfig config = createSamplePushConfig(
          "http://url.com/callback", "cfg1", "token");
      pushNotificationConfigStore.setInfo(taskId, config);

      ListTaskPushNotificationConfigParams params =
          new ListTaskPushNotificationConfigParams(taskId, 0, "", tenant);
      ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

      assertNotNull(result);
      assertEquals(1, result.configs().size());
      assertEquals(tenant, result.configs().get(0).tenant(),
          "Tenant from params should be returned in result");
    }

    @Test
    @Transactional
    public void testPaginationOrderingConsistency() {
      String taskId = "task_ordering_consistency_" + System.currentTimeMillis();
      createSamples(taskId, 15);

      List<String> allConfigIds = new java.util.ArrayList<>();
      String pageToken = "";
      int pageCount = 0;

      do {
        ListTaskPushNotificationConfigParams params =
            new ListTaskPushNotificationConfigParams(taskId, 3, pageToken, "");
        ListTaskPushNotificationConfigResult result = pushNotificationConfigStore.getInfo(params);

        result.configs().forEach(c ->
            allConfigIds.add(c.config().id()));
        pageToken = result.nextPageToken();
        pageCount++;

        assertTrue(pageCount <= 20, "Should not have more than 20 pages for 15 configs");

      } while (pageToken != null);

      assertEquals(15, allConfigIds.size(), "Should retrieve all 15 configs");
      assertEquals(15, new java.util.HashSet<>(allConfigIds).size(),
          "All config IDs should be unique - no duplicates");

      assertEquals("cfg14", allConfigIds.get(0),
          "First config should be most recently created (DESC order)");
      assertEquals("cfg0", allConfigIds.get(14),
          "Last config should be oldest created");
    }
  private void createSamples(String taskId, int size) {
        // Create configs with slight delays to ensure unique timestamps for deterministic ordering
        for (int i = 0; i < size; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            pushNotificationConfigStore.setInfo(taskId, config);

            // Sleep briefly to ensure each config gets a unique timestamp
            // This prevents non-deterministic ordering in pagination tests
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while creating test samples", e);
            }
        }
    }
}
