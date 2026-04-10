package org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa;

import static org.a2aproject.sdk.client.http.A2AHttpClient.APPLICATION_JSON;
import static org.a2aproject.sdk.client.http.A2AHttpClient.CONTENT_TYPE;
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

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.server.tasks.BasePushNotificationSender;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
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

    private TaskPushNotificationConfig createSamplePushConfig(String url, String configId, String token) {
        return TaskPushNotificationConfig.builder()
                .url(url)
                .id(configId)
                .token(token)
                .build();
    }

    @Test
    @Transactional
    public void testSetInfoAddsNewConfig() {
        String taskId = "task_new";
        TaskPushNotificationConfig config = createSamplePushConfig("http://new.url/callback", "cfg1", null);

        TaskPushNotificationConfig result = configStore.setInfo(TaskPushNotificationConfig.builder(config).taskId(taskId).build());

        assertNotNull(result);
        assertEquals(config.url(), result.url());
        assertEquals(config.id(), result.id());

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());
        assertEquals(config.url(), configResult.configs().get(0).url());
        assertEquals(config.id(), configResult.configs().get(0).id());
    }

    @Test
    @Transactional
    public void testSetInfoAppendsToExistingConfig() {
        String taskId = "task_update";
        TaskPushNotificationConfig initialConfig = createSamplePushConfig(
                "http://initial.url/callback", "cfg_initial", null);
        configStore.setInfo(TaskPushNotificationConfig.builder(initialConfig).taskId(taskId).build());

        TaskPushNotificationConfig updatedConfig = createSamplePushConfig(
                "http://updated.url/callback", "cfg_updated", null);
        configStore.setInfo(TaskPushNotificationConfig.builder(updatedConfig).taskId(taskId).build());

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertEquals(2, configResult.configs().size());

        // Find the configs by ID since order might vary
        List<TaskPushNotificationConfig> configs = configResult.configs();
        TaskPushNotificationConfig foundInitial = configs.stream()
                .filter(c -> "cfg_initial".equals(c.id()))
                .findFirst()
                .orElse(null);
        TaskPushNotificationConfig foundUpdated = configs.stream()
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
        TaskPushNotificationConfig initialConfig = TaskPushNotificationConfig.builder()
                .id("")
                .url("http://initial.url/callback")
                .taskId(taskId)
                .build();

        TaskPushNotificationConfig result = configStore.setInfo(initialConfig);
        assertEquals(taskId, result.id(), "Config ID should default to taskId when not provided");

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertEquals(1, configResult.configs().size());
        assertEquals(taskId, configResult.configs().get(0).id());

        TaskPushNotificationConfig updatedConfig = TaskPushNotificationConfig.builder()
                .id("")
                .url("http://initial.url/callback_new")
                .taskId(taskId)
                .build();

        TaskPushNotificationConfig updatedResult = configStore.setInfo(updatedConfig);
        assertEquals(taskId, updatedResult.id());

        configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertEquals(1, configResult.configs().size(), "Should replace existing config with same ID rather than adding new one");
        assertEquals(updatedConfig.url(), configResult.configs().get(0).url());
    }

    @Test
    @Transactional
    public void testGetInfoExistingConfig() {
        String taskId = "task_get_exist";
        TaskPushNotificationConfig config = createSamplePushConfig("http://get.this/callback", "cfg1", null);
        configStore.setInfo(TaskPushNotificationConfig.builder(config).taskId(taskId).build());

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());
        assertEquals(config.url(), configResult.configs().get(0).url());
        assertEquals(config.id(), configResult.configs().get(0).id());
    }

    @Test
    @Transactional
    public void testGetInfoNonExistentConfig() {
        String taskId = "task_get_non_exist";
        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list for non-existent task ID");
    }

    @Test
    @Transactional
    public void testDeleteInfoExistingConfig() {
        String taskId = "task_delete_exist";
        TaskPushNotificationConfig config = createSamplePushConfig("http://delete.this/callback", "cfg1", null);
        configStore.setInfo(TaskPushNotificationConfig.builder(config).taskId(taskId).build());

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());

        configStore.deleteInfo(taskId, config.id());

        ListTaskPushNotificationConfigsResult configsAfterDelete = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configsAfterDelete);
        assertTrue(configsAfterDelete.configs().isEmpty(), "Should return empty list when no configs remain after deletion");
    }

    @Test
    @Transactional
    public void testDeleteInfoNonExistentConfig() {
        String taskId = "task_delete_non_exist";
        // Should not throw an error
        configStore.deleteInfo(taskId, "non_existent_id");

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list for non-existent task ID");
    }

    @Test
    @Transactional
    public void testDeleteInfoWithNullConfigId() {
        String taskId = "task_delete_null_config";
        TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                .id("")
                .url("http://delete.this/callback")
                .taskId(taskId)
                .build();
        configStore.setInfo(config);

        // Delete with null configId should use taskId
        configStore.deleteInfo(taskId, null);

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertTrue(configResult.configs().isEmpty(), "Should return empty list after deletion when using taskId as configId");
    }

    @Test
    @Transactional
    public void testSendNotificationSuccess() throws Exception {
        String taskId = "task_send_success";
        Task task = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);
        TaskPushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);
        configStore.setInfo(TaskPushNotificationConfig.builder(config).taskId(taskId).build());

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
        assertTrue(sentBody.contains(task.status().state().name()));
    }

    @Test
    @Transactional
    @Disabled("Token authentication is not yet implemented in BasePushNotificationSender (TODO auth)")
    public void testSendNotificationWithToken() throws Exception {
        String taskId = "task_send_with_token";
        Task task = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);
        TaskPushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");
        configStore.setInfo(TaskPushNotificationConfig.builder(config).taskId(taskId).build());

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
        assertTrue(sentBody.contains(task.status().state().name()));
    }

    @Test
    @Transactional
    public void testSendNotificationNoConfig() throws Exception {
        String taskId = "task_send_no_config";
        Task task = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);

        notificationSender.sendNotification(task);

        // Verify HTTP client was never called
        verify(mockHttpClient, never()).createPost();
    }

    @Test
    @Transactional
    public void testMultipleConfigsForSameTask() {
        String taskId = "task_multiple";
        TaskPushNotificationConfig config1 = createSamplePushConfig("http://url1.com/callback", "cfg1", null);
        TaskPushNotificationConfig config2 = createSamplePushConfig("http://url2.com/callback", "cfg2", null);

        configStore.setInfo(TaskPushNotificationConfig.builder(config1).taskId(taskId).build());
        configStore.setInfo(TaskPushNotificationConfig.builder(config2).taskId(taskId).build());

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertEquals(2, configResult.configs().size());

        // Verify both configs are present
        assertTrue(configResult.configs().stream().anyMatch(c -> "cfg1".equals(c.id())));
        assertTrue(configResult.configs().stream().anyMatch(c -> "cfg2".equals(c.id())));
    }

    @Test
    @Transactional
    public void testDeleteSpecificConfigFromMultiple() {
        String taskId = "task_delete_specific";
        TaskPushNotificationConfig config1 = createSamplePushConfig("http://url1.com/callback", "cfg1", null);
        TaskPushNotificationConfig config2 = createSamplePushConfig("http://url2.com/callback", "cfg2", null);

        configStore.setInfo(TaskPushNotificationConfig.builder(config1).taskId(taskId).build());
        configStore.setInfo(TaskPushNotificationConfig.builder(config2).taskId(taskId).build());

        // Delete only config1
        configStore.deleteInfo(taskId, "cfg1");

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertNotNull(configResult);
        assertEquals(1, configResult.configs().size());
        assertEquals("cfg2", configResult.configs().get(0).id());
    }

    @Test
    @Transactional
    public void testConfigStoreIntegration() {
        String taskId = "integration_test";
        TaskPushNotificationConfig config = createSamplePushConfig("http://example.com", "test_id", "test_token");

        // Test that we can store and retrieve configurations
        TaskPushNotificationConfig storedConfig = configStore.setInfo(TaskPushNotificationConfig.builder(config).taskId(taskId).build());
        assertEquals(config.url(), storedConfig.url());
        assertEquals(config.token(), storedConfig.token());

        ListTaskPushNotificationConfigsResult configResult = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertEquals(1, configResult.configs().size());
        assertEquals(config.url(), configResult.configs().get(0).url());

        // Test deletion
        configStore.deleteInfo(taskId, storedConfig.id());
        ListTaskPushNotificationConfigsResult afterDeletion = configStore.getInfo(new ListTaskPushNotificationConfigsParams(taskId));
        assertTrue(afterDeletion.configs().isEmpty());
    }
}
