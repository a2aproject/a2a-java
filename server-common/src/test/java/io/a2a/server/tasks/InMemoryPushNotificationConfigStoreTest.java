package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;

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

    private Task createSampleTask(String taskId, TaskState state) {
        return new Task.Builder()
                .id(taskId)
                .contextId("ctx456")
                .status(new TaskStatus(state))
                .build();
    }

    private PushNotificationConfig createSamplePushConfig(String url, String configId, String token) {
        return new PushNotificationConfig.Builder()
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

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertNotNull(configs);
        assertEquals(1, configs.size());
        assertEquals(config.url(), configs.get(0).url());
        assertEquals(config.id(), configs.get(0).id());
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

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertNotNull(configs);
        assertEquals(2, configs.size());

        // Find the configs by ID since order might vary
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
        PushNotificationConfig initialConfig = new PushNotificationConfig.Builder()
                .url("http://initial.url/callback")
                .build(); // No ID set

        PushNotificationConfig result = configStore.setInfo(taskId, initialConfig);
        assertEquals(taskId, result.id(), "Config ID should default to taskId when not provided");

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertEquals(1, configs.size());
        assertEquals(taskId, configs.get(0).id());

        PushNotificationConfig updatedConfig = new PushNotificationConfig.Builder()
                .url("http://initial.url/callback_new")
                .build(); // No ID set

        PushNotificationConfig updatedResult = configStore.setInfo(taskId, updatedConfig);
        assertEquals(taskId, updatedResult.id());

        configs = configStore.getInfo(taskId);
        assertEquals(1, configs.size(), "Should replace existing config with same ID rather than adding new one");
        assertEquals(updatedConfig.url(), configs.get(0).url());
    }

    @Test
    public void testGetInfoExistingConfig() {
        String taskId = "task_get_exist";
        PushNotificationConfig config = createSamplePushConfig("http://get.this/callback", "cfg1", null);
        configStore.setInfo(taskId, config);

        List<PushNotificationConfig> retrievedConfigs = configStore.getInfo(taskId);
        assertNotNull(retrievedConfigs);
        assertEquals(1, retrievedConfigs.size());
        assertEquals(config.url(), retrievedConfigs.get(0).url());
        assertEquals(config.id(), retrievedConfigs.get(0).id());
    }

    @Test
    public void testGetInfoNonExistentConfig() {
        String taskId = "task_get_non_exist";
        List<PushNotificationConfig> retrievedConfigs = configStore.getInfo(taskId);
        assertNull(retrievedConfigs, "Should return null for non-existent task ID");
    }

    @Test
    public void testDeleteInfoExistingConfig() {
        String taskId = "task_delete_exist";
        PushNotificationConfig config = createSamplePushConfig("http://delete.this/callback", "cfg1", null);
        configStore.setInfo(taskId, config);

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertNotNull(configs);
        assertEquals(1, configs.size());

        configStore.deleteInfo(taskId, config.id());

        List<PushNotificationConfig> configsAfterDelete = configStore.getInfo(taskId);
        assertNull(configsAfterDelete, "Should return null when no configs remain after deletion");
    }

    @Test
    public void testDeleteInfoNonExistentConfig() {
        String taskId = "task_delete_non_exist";
        // Should not throw an error
        configStore.deleteInfo(taskId, "non_existent_id");

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertNull(configs, "Should still return null for non-existent task ID");
    }

    @Test
    public void testDeleteInfoWithNullConfigId() {
        String taskId = "task_delete_null_config";
        PushNotificationConfig config = new PushNotificationConfig.Builder()
                .url("http://delete.this/callback")
                .build(); // No ID set, will use taskId
        configStore.setInfo(taskId, config);

        // Delete with null configId should use taskId
        configStore.deleteInfo(taskId, null);

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertNull(configs, "Should return null after deletion when using taskId as configId");
    }

    @Test
    public void testSendNotificationSuccess() throws Exception {
        String taskId = "task_send_success";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);
        configStore.setInfo(taskId, config);

        // Mock successful HTTP response
        when(mockHttpClient.createPost()).thenReturn(mockPostBuilder);
        when(mockPostBuilder.url(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.body(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.post()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.success()).thenReturn(true);

        notificationSender.sendNotification(task);

        // Verify HTTP client was called
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).createPost();
        verify(mockPostBuilder).url(config.url());
        verify(mockPostBuilder).body(bodyCaptor.capture());
        verify(mockPostBuilder).post();
        
        // Verify the request body contains the task data
        String sentBody = bodyCaptor.getValue();
        assertTrue(sentBody.contains(task.getId()));
        assertTrue(sentBody.contains(task.getStatus().state().asString()));
    }

    @Test
    @Disabled("Token authentication is not yet implemented in BasePushNotificationSender (TODO auth)")
    public void testSendNotificationWithToken() throws Exception {
        String taskId = "task_send_with_token";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");
        configStore.setInfo(taskId, config);

        // Mock successful HTTP response
        when(mockHttpClient.createPost()).thenReturn(mockPostBuilder);
        when(mockPostBuilder.url(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.body(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.post()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.success()).thenReturn(true);

        notificationSender.sendNotification(task);

        // TODO: Once token authentication is implemented, verify that:
        // 1. The token is included in request headers (e.g., X-A2A-Notification-Token)
        // 2. The HTTP client is called with proper authentication
        // 3. The token from the config is actually used
        
        // For now, just verify basic HTTP client interaction
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).createPost();
        verify(mockPostBuilder).url(config.url());
        verify(mockPostBuilder).body(bodyCaptor.capture());
        verify(mockPostBuilder).post();
        
        // Verify the request body contains the task data
        String sentBody = bodyCaptor.getValue();
        assertTrue(sentBody.contains(task.getId()));
        assertTrue(sentBody.contains(task.getStatus().state().asString()));
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
    public void testMultipleConfigsForSameTask() {
        String taskId = "task_multiple";
        PushNotificationConfig config1 = createSamplePushConfig("http://url1.com/callback", "cfg1", null);
        PushNotificationConfig config2 = createSamplePushConfig("http://url2.com/callback", "cfg2", null);

        configStore.setInfo(taskId, config1);
        configStore.setInfo(taskId, config2);

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertNotNull(configs);
        assertEquals(2, configs.size());

        // Verify both configs are present
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

        List<PushNotificationConfig> configs = configStore.getInfo(taskId);
        assertNotNull(configs);
        assertEquals(1, configs.size());
        assertEquals("cfg2", configs.get(0).id());
    }

    @Test
    public void testConfigStoreIntegration() {
        String taskId = "integration_test";
        PushNotificationConfig config = createSamplePushConfig("http://example.com", "test_id", "test_token");

        // Test that we can store and retrieve configurations
        PushNotificationConfig storedConfig = configStore.setInfo(taskId, config);
        assertEquals(config.url(), storedConfig.url());
        assertEquals(config.token(), storedConfig.token());

        List<PushNotificationConfig> retrievedConfigs = configStore.getInfo(taskId);
        assertEquals(1, retrievedConfigs.size());
        assertEquals(config.url(), retrievedConfigs.get(0).url());

        // Test deletion
        configStore.deleteInfo(taskId, storedConfig.id());
        List<PushNotificationConfig> afterDeletion = configStore.getInfo(taskId);
        assertNull(afterDeletion);
    }

}
