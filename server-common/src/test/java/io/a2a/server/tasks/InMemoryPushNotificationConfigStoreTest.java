package io.a2a.server.tasks;

import static io.a2a.client.http.A2AHttpClient.APPLICATION_JSON;
import static io.a2a.client.http.A2AHttpClient.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.common.A2AHeaders;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class InMemoryPushNotificationConfigStoreTest {

    private InMemoryPushNotificationConfigStore configStore;
    private BasePushNotificationSender notificationSender;

    @Mock
    private A2AHttpClient mockHttpClient;

    @Mock
    private A2AHttpClient.PostBuilder mockPostBuilder;

    @Mock
    private A2AHttpResponse mockHttpResponse;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        configStore = new InMemoryPushNotificationConfigStore();
        notificationSender = new BasePushNotificationSender(configStore, mockHttpClient);
    }

    private void setupBasicMockHttpResponse() throws Exception {
        when(mockHttpClient.createPost()).thenReturn(mockPostBuilder);
        when(mockPostBuilder.url(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.addHeader(CONTENT_TYPE, APPLICATION_JSON)).thenReturn(mockPostBuilder);
        when(mockPostBuilder.body(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.post()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.success()).thenReturn(true);
    }

    private void verifyHttpCallWithoutToken(PushNotificationConfig config, Task task, String expectedToken) throws Exception {
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).createPost();
        verify(mockPostBuilder).url(config.url());
        verify(mockPostBuilder).body(bodyCaptor.capture());
        verify(mockPostBuilder).post();
        // Verify that addHeader was never called for authentication token
        verify(mockPostBuilder, never()).addHeader(A2AHeaders.X_A2A_NOTIFICATION_TOKEN, expectedToken);
        
        // Verify the request body contains the task data
        String sentBody = bodyCaptor.getValue();
        assertTrue(sentBody.contains(task.id()));
        assertTrue(sentBody.contains(task.status().state().asString()));
    }

    private Task createSampleTask(String taskId, TaskState state) {
        return Task.builder()
                .id(taskId)
                .contextId("ctx456")
                .status(new TaskStatus(state))
                .build();
    }

    private PushNotificationConfig createSamplePushConfig(String url, String configId, String token) {
        return PushNotificationConfig.builder()
                .url(url)
                .id(configId)
                .token(token)
                .build();
    }

    @Test
    public void testSetInfoAddsNewConfig() {
        String taskId = "task_new";
        PushNotificationConfig config = createSamplePushConfig("http://new.url/callback", "cfg1", null);

        PushNotificationConfig result = configStore.setInfo(taskId, config);

        assertNotNull(result);
        assertEquals(config.url(), result.url());
        assertEquals(config.id(), result.id());

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());
        assertEquals(config.url(), configResult.configs().get(0).config().url());
        assertEquals(config.id(), configResult.configs().get(0).config().id());
    }

    @Test
    public void testSetInfoAppendsToExistingConfig() {
        String taskId = "task_update";
        PushNotificationConfig initialConfig = createSamplePushConfig(
                "http://initial.url/callback", "cfg_initial", null);
        configStore.setInfo(taskId, initialConfig);

        PushNotificationConfig updatedConfig = createSamplePushConfig(
                "http://updated.url/callback", "cfg_updated", null);
        configStore.setInfo(taskId, updatedConfig);

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertEquals(2, configResult.configs().size());

        // Find the configs by ID since order might vary
        List<PushNotificationConfig> configs = configResult.configs().stream()
                .map(TaskPushNotificationConfig::config)
                .toList();
        PushNotificationConfig foundInitial = configs.stream()
                .filter(c -> "cfg_initial".equals(c.id()))
                .findFirst()
                .orElse(null);
        PushNotificationConfig foundUpdated = configs.stream()
                .filter(c -> "cfg_updated".equals(c.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(foundInitial);
        assertNotNull(foundUpdated);
        assertEquals(initialConfig.url(), foundInitial.url());
        assertEquals(updatedConfig.url(), foundUpdated.url());
    }

    @Test
    public void testSetInfoWithoutConfigId() {
        String taskId = "task1";
        PushNotificationConfig initialConfig = PushNotificationConfig.builder()
                .url("http://initial.url/callback")
                .build(); // No ID set

        PushNotificationConfig result = configStore.setInfo(taskId, initialConfig);
        assertEquals(taskId, result.id(), "Config ID should default to taskId when not provided");

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertEquals(1, configResult.configs().size());
        assertEquals(taskId, configResult.configs().get(0).config().id());

        PushNotificationConfig updatedConfig = PushNotificationConfig.builder()
                .url("http://initial.url/callback_new")
                .build(); // No ID set

        PushNotificationConfig updatedResult = configStore.setInfo(taskId, updatedConfig);
        assertEquals(taskId, updatedResult.id());

        configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertEquals(1, configResult.configs().size(), "Should replace existing config with same ID rather than adding new one");
        assertEquals(updatedConfig.url(), configResult.configs().get(0).config().url());
    }

    @Test
    public void testGetInfoExistingConfig() {
        String taskId = "task_get_exist";
        PushNotificationConfig config = createSamplePushConfig("http://get.this/callback", "cfg1", null);
        configStore.setInfo(taskId, config);

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());
        assertEquals(config.url(), configResult.configs().get(0).config().url());
        assertEquals(config.id(), configResult.configs().get(0).config().id());
    }

    @Test
    public void testGetInfoNonExistentConfig() {
        String taskId = "task_get_non_exist";
        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list for non-existent task ID");
    }

    @Test
    public void testDeleteInfoExistingConfig() {
        String taskId = "task_delete_exist";
        PushNotificationConfig config = createSamplePushConfig("http://delete.this/callback", "cfg1", null);
        configStore.setInfo(taskId, config);

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());

        configStore.deleteInfo(taskId, config.id());

        ListTaskPushNotificationConfigResult configsAfterDelete = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configsAfterDelete);
        assertTrue(configsAfterDelete.configs().isEmpty(), "Should return empty list when no configs remain after deletion");
    }

    @Test
    public void testDeleteInfoNonExistentConfig() {
        String taskId = "task_delete_non_exist";
        // Should not throw an error
        configStore.deleteInfo(taskId, "non_existent_id");

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list for non-existent task ID");
    }

    @Test
    public void testDeleteInfoWithNullConfigId() {
        String taskId = "task_delete_null_config";
        PushNotificationConfig config = PushNotificationConfig.builder()
                .url("http://delete.this/callback")
                .build(); // No ID set, will use taskId
        configStore.setInfo(taskId, config);

        // Delete with null configId should use taskId
        configStore.deleteInfo(taskId, null);

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list after deletion when using taskId as configId");
    }

    @Test
    public void testSendNotificationSuccess() throws Exception {
        String taskId = "task_send_success";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);
        configStore.setInfo(taskId, config);

        // Mock successful HTTP response
        setupBasicMockHttpResponse();

        notificationSender.sendNotification(task);

        // Verify HTTP client was called
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).createPost();
        verify(mockPostBuilder).url(config.url());
        verify(mockPostBuilder).body(bodyCaptor.capture());
        verify(mockPostBuilder).post();

        // Verify the request body contains the task data
        String sentBody = bodyCaptor.getValue();
        assertTrue(sentBody.contains(task.id()));
        assertTrue(sentBody.contains(task.status().state().asString()));
    }

    @Test
    public void testSendNotificationWithToken() throws Exception {
        String taskId = "task_send_with_token";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");
        configStore.setInfo(taskId, config);

        // Mock successful HTTP response
        when(mockHttpClient.createPost()).thenReturn(mockPostBuilder);
        when(mockPostBuilder.url(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.body(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.addHeader(any(String.class), any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.post()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.success()).thenReturn(true);

        notificationSender.sendNotification(task);

        // Verify HTTP client was called with proper authentication
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).createPost();
        verify(mockPostBuilder).url(config.url());
        verify(mockPostBuilder).body(bodyCaptor.capture());
        // Verify that the token is included in request headers as X-A2A-Notification-Token
        verify(mockPostBuilder).addHeader(A2AHeaders.X_A2A_NOTIFICATION_TOKEN, config.token());
        verify(mockPostBuilder).post();
        
        // Verify the request body contains the task data
        String sentBody = bodyCaptor.getValue();
        assertTrue(sentBody.contains(task.id()));
        assertTrue(sentBody.contains(task.status().state().asString()));
    }

    @Test
    public void testSendNotificationNoConfig() throws Exception {
        String taskId = "task_send_no_config";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);

        notificationSender.sendNotification(task);

        // Verify HTTP client was never called
        verify(mockHttpClient, never()).createPost();
    }

    @Test
    public void testSendNotificationWithEmptyToken() throws Exception {
        String taskId = "task_send_empty_token";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "");
        configStore.setInfo(taskId, config);

        setupBasicMockHttpResponse();
        notificationSender.sendNotification(task);
        verifyHttpCallWithoutToken(config, task, "");
    }

    @Test
    public void testSendNotificationWithBlankToken() throws Exception {
        String taskId = "task_send_blank_token";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "   ");
        configStore.setInfo(taskId, config);

        setupBasicMockHttpResponse();
        notificationSender.sendNotification(task);
        verifyHttpCallWithoutToken(config, task, "   ");
    }

    @Test
    public void testMultipleConfigsForSameTask() {
        String taskId = "task_multiple";
        PushNotificationConfig config1 = createSamplePushConfig("http://url1.com/callback", "cfg1", null);
        PushNotificationConfig config2 = createSamplePushConfig("http://url2.com/callback", "cfg2", null);

        configStore.setInfo(taskId, config1);
        configStore.setInfo(taskId, config2);

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertEquals(2, configResult.configs().size());

        // Verify both configs are present
        List<PushNotificationConfig> configs = configResult.configs().stream()
                .map(TaskPushNotificationConfig::config)
                .toList();
        assertTrue(configs.stream().anyMatch(c -> "cfg1".equals(c.id())));
        assertTrue(configs.stream().anyMatch(c -> "cfg2".equals(c.id())));
    }

    @Test
    public void testDeleteSpecificConfigFromMultiple() {
        String taskId = "task_delete_specific";
        PushNotificationConfig config1 = createSamplePushConfig("http://url1.com/callback", "cfg1", null);
        PushNotificationConfig config2 = createSamplePushConfig("http://url2.com/callback", "cfg2", null);

        configStore.setInfo(taskId, config1);
        configStore.setInfo(taskId, config2);

        // Delete only config1
        configStore.deleteInfo(taskId, "cfg1");

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());
        assertEquals("cfg2", configResult.configs().get(0).config().id());
    }

    @Test
    public void testConfigStoreIntegration() {
        String taskId = "integration_test";
        PushNotificationConfig config = createSamplePushConfig("http://example.com", "test_id", "test_token");

        // Test that we can store and retrieve configurations
        PushNotificationConfig storedConfig = configStore.setInfo(taskId, config);
        assertEquals(config.url(), storedConfig.url());
        assertEquals(config.token(), storedConfig.token());

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertEquals(1, configResult.configs().size());
        assertEquals(config.url(), configResult.configs().get(0).config().url());

        // Test deletion
        configStore.deleteInfo(taskId, storedConfig.id());
        ListTaskPushNotificationConfigResult afterDeletion = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(afterDeletion);
        assertTrue(afterDeletion.configs().isEmpty());
    }

    @Test
    public void testPaginationWithPageSize() {
        String taskId = "task_pagination";
        // Create 5 configs
        for (int i = 0; i < 5; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Request first page with pageSize=2
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

        assertNotNull(result);
        assertEquals(2, result.configs().size(), "Should return 2 configs");
        assertNotNull(result.nextPageToken(), "Should have nextPageToken when more items exist");
    }

    @Test
    public void testPaginationWithPageToken() {
        String taskId = "task_pagination_token";
        // Create 5 configs
        for (int i = 0; i < 5; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Get first page
        ListTaskPushNotificationConfigParams firstPageParams = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult firstPage = configStore.getInfo(firstPageParams);
        assertNotNull(firstPage.nextPageToken());

        // Get second page using nextPageToken
        ListTaskPushNotificationConfigParams secondPageParams = new ListTaskPushNotificationConfigParams(
                taskId, 2, firstPage.nextPageToken(), "");
        ListTaskPushNotificationConfigResult secondPage = configStore.getInfo(secondPageParams);

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
        // Since configs are created in order, we can verify the IDs
        assertEquals("cfg0", firstPageIds.get(0));
        assertEquals("cfg1", firstPageIds.get(1));
        assertEquals("cfg2", secondPageIds.get(0));
        assertEquals("cfg3", secondPageIds.get(1));
    }

    @Test
    public void testPaginationLastPage() {
        String taskId = "task_pagination_last";
        // Create 5 configs
        for (int i = 0; i < 5; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Get first page (2 items)
        ListTaskPushNotificationConfigParams firstPageParams = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult firstPage = configStore.getInfo(firstPageParams);

        // Get second page (2 items)
        ListTaskPushNotificationConfigParams secondPageParams = new ListTaskPushNotificationConfigParams(
                taskId, 2, firstPage.nextPageToken(), "");
        ListTaskPushNotificationConfigResult secondPage = configStore.getInfo(secondPageParams);

        // Get last page (1 item remaining)
        ListTaskPushNotificationConfigParams lastPageParams = new ListTaskPushNotificationConfigParams(
                taskId, 2, secondPage.nextPageToken(), "");
        ListTaskPushNotificationConfigResult lastPage = configStore.getInfo(lastPageParams);

        assertNotNull(lastPage);
        assertEquals(1, lastPage.configs().size(), "Last page should have 1 remaining config");
        assertNull(lastPage.nextPageToken(), "Last page should not have nextPageToken");
    }

    @Test
    public void testPaginationWithZeroPageSize() {
        String taskId = "task_pagination_zero";
        // Create 5 configs
        for (int i = 0; i < 5; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Request with pageSize=0 should return all configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 0, "", "");
        ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

        assertNotNull(result);
        assertEquals(5, result.configs().size(), "Should return all 5 configs when pageSize=0");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when returning all");
    }

    @Test
    public void testPaginationWithNegativePageSize() {
        String taskId = "task_pagination_negative";
        // Create 3 configs
        for (int i = 0; i < 3; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Request with negative pageSize should return all configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, -1, "", "");
        ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

        assertNotNull(result);
        assertEquals(3, result.configs().size(), "Should return all configs when pageSize is negative");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when returning all");
    }

    @Test
    public void testPaginationPageSizeLargerThanConfigs() {
        String taskId = "task_pagination_large";
        // Create 3 configs
        for (int i = 0; i < 3; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Request with pageSize larger than available configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 10, "", "");
        ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

        assertNotNull(result);
        assertEquals(3, result.configs().size(), "Should return all 3 configs");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when all configs fit in one page");
    }

    @Test
    public void testPaginationExactlyPageSize() {
        String taskId = "task_pagination_exact";
        // Create exactly 3 configs
        for (int i = 0; i < 3; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Request with pageSize equal to number of configs
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 3, "", "");
        ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

        assertNotNull(result);
        assertEquals(3, result.configs().size(), "Should return all 3 configs");
        assertNull(result.nextPageToken(), "Should not have nextPageToken when configs exactly match pageSize");
    }

    @Test
    public void testPaginationWithInvalidToken() {
        String taskId = "task_pagination_invalid_token";
        // Create 5 configs
        for (int i = 0; i < 5; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Request with invalid pageToken - implementation behavior is to start from beginning
        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(
                taskId, 2, "invalid_token_that_does_not_exist", "");
        ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

        assertNotNull(result);
        // When token is not found, implementation starts from beginning
        assertEquals(2, result.configs().size(), "Should return first page when token is not found");
        assertNotNull(result.nextPageToken(), "Should have nextPageToken since more items exist");
    }

    @Test
    public void testPaginationEmptyTaskWithPageSize() {
        String taskId = "task_pagination_empty";
        // No configs created

        ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 2, "", "");
        ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

        assertNotNull(result);
        assertTrue(result.configs().isEmpty(), "Should return empty list for non-existent task");
        assertNull(result.nextPageToken(), "Should not have nextPageToken for empty result");
    }

    @Test
    public void testPaginationFullIteration() {
        String taskId = "task_pagination_full";
        // Create 7 configs
        for (int i = 0; i < 7; i++) {
            PushNotificationConfig config = createSamplePushConfig(
                    "http://url" + i + ".com/callback", "cfg" + i, "token" + i);
            configStore.setInfo(taskId, config);
        }

        // Iterate through all pages with pageSize=3
        int totalCollected = 0;
        String pageToken = "";
        int pageCount = 0;

        do {
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, 3, pageToken, "");
            ListTaskPushNotificationConfigResult result = configStore.getInfo(params);

            totalCollected += result.configs().size();
            pageToken = result.nextPageToken();
            pageCount++;

            // Safety check to prevent infinite loop
            assertTrue(pageCount <= 10, "Should not have more than 10 pages for 7 configs");

        } while (pageToken != null);

        assertEquals(7, totalCollected, "Should collect all 7 configs across all pages");
        assertEquals(3, pageCount, "Should have exactly 3 pages (3+3+1)");
    }

}
