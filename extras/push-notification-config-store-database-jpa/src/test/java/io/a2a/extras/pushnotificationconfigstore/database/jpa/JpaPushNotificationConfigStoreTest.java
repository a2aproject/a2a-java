package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import static io.a2a.client.http.A2AHttpClient.APPLICATION_JSON;
import static io.a2a.client.http.A2AHttpClient.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@QuarkusTest
public class JpaPushNotificationConfigStoreTest {

    @Inject
    PushNotificationConfigStore configStore;

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
        notificationSender = new BasePushNotificationSender(configStore, mockHttpClient);
    }

    @Test
    public void testIsJpaDatabasePushNotificationConfigStore() {
        assertInstanceOf(JpaDatabasePushNotificationConfigStore.class, configStore);
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
    public void testGetInfoNonExistentConfig() {
        String taskId = "task_get_non_exist";
        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list for non-existent task ID");
    }

    @Test
    @Transactional
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
    @Transactional
    public void testDeleteInfoNonExistentConfig() {
        String taskId = "task_delete_non_exist";
        // Should not throw an error
        configStore.deleteInfo(taskId, "non_existent_id");

        ListTaskPushNotificationConfigResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list for non-existent task ID");
    }

    @Test
    @Transactional
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
    @Transactional
    public void testSendNotificationSuccess() throws Exception {
        String taskId = "task_send_success";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);
        configStore.setInfo(taskId, config);

        // Mock successful HTTP response
        when(mockHttpClient.createPost()).thenReturn(mockPostBuilder);
        when(mockPostBuilder.url(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.addHeader(CONTENT_TYPE, APPLICATION_JSON)).thenReturn(mockPostBuilder);
        when(mockPostBuilder.body(any(String.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.post()).thenReturn(mockHttpResponse);
        when(mockHttpResponse.success()).thenReturn(true);

        notificationSender.sendNotification(task);

        // Verify HTTP client was called
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpClient).createPost();
        verify(mockPostBuilder).url(config.url());
        verify(mockPostBuilder).addHeader(CONTENT_TYPE, APPLICATION_JSON);
        verify(mockPostBuilder).body(bodyCaptor.capture());
        verify(mockPostBuilder).post();

        // Verify the request body contains the task data
        String sentBody = bodyCaptor.getValue();
        assertTrue(sentBody.contains(task.id()));
        assertTrue(sentBody.contains(task.status().state().asString()));
    }

    @Test
    @Transactional
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
        assertTrue(sentBody.contains(task.id()));
        assertTrue(sentBody.contains(task.status().state().asString()));
    }

    @Test
    @Transactional
    public void testSendNotificationNoConfig() throws Exception {
        String taskId = "task_send_no_config";
        Task task = createSampleTask(taskId, TaskState.COMPLETED);

        notificationSender.sendNotification(task);

        // Verify HTTP client was never called
        verify(mockHttpClient, never()).createPost();
    }

    @Test
    @Transactional
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
    @Transactional
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
    @Transactional
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
        assertTrue(afterDeletion.configs().isEmpty());
    }
}
