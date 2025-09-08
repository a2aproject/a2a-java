package io.a2a.server.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpResponse;
import io.a2a.client.http.sse.Event;
import io.a2a.server.http.HttpClientManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.a2a.common.A2AHeaders;
import io.a2a.util.Utils;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PushNotificationSenderTest {

    @Mock
    private HttpClientManager clientManager;

    private TestHttpClient testHttpClient;
    private InMemoryPushNotificationConfigStore configStore;
    private BasePushNotificationSender sender;

    /**
     * Simple test implementation of A2AHttpClient that captures HTTP calls for verification
     */
    private static class TestHttpClient implements HttpClient {
        final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
        final List<String> urls = Collections.synchronizedList(new ArrayList<>());
        final List<Map<String, String>> headers = Collections.synchronizedList(new ArrayList<>());
        volatile CountDownLatch latch;
        volatile boolean shouldThrowException = false;

        @Override
        public GetRequestBuilder get(String path) {
            return null;
        }

        @Override
        public PostRequestBuilder post(String path) {
            return new TestPostBuilder();
        }

        @Override
        public DeleteRequestBuilder delete(String path) {
            return null;
        }

        class TestPostBuilder implements HttpClient.PostRequestBuilder {
            private volatile String body;
            private final Map<String, String> requestHeaders = new java.util.HashMap<>();

            @Override
            public PostRequestBuilder body(String body) {
                this.body = body;
                return this;
            }

            @Override
            public CompletableFuture<HttpResponse> send() {
                CompletableFuture<HttpResponse> future = new CompletableFuture<>();

                if (shouldThrowException) {
                    future.completeExceptionally(new IOException("Simulated network error"));
                    return future;
                }
                
                try {
                    Task task = Utils.OBJECT_MAPPER.readValue(body, Task.TYPE_REFERENCE);
                    tasks.add(task);
                    headers.add(new java.util.HashMap<>(requestHeaders));

                    future.complete(
                        new HttpResponse() {
                            @Override
                            public int statusCode() {
                                return 200;
                            }

                            @Override
                            public boolean success() {
                                return true;
                            }

                            @Override
                            public String body() {
                                return "";
                            }

                            @Override
                            public void bodyAsSse(Consumer<Event> eventConsumer, Consumer<Throwable> errorConsumer) {

                            }
                        });
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    if (latch != null) {
                        latch.countDown();
                    }
                }

                return future;
            }

            @Override
            public PostRequestBuilder addHeader(String name, String value) {
                requestHeaders.put(name, value);
                return this;
            }

            @Override
            public PostRequestBuilder addHeaders(Map<String, String> headers) {
                requestHeaders.putAll(headers);
                return this;
            }
        }
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testHttpClient = new TestHttpClient();
        configStore = new InMemoryPushNotificationConfigStore();
        sender = new BasePushNotificationSender(configStore, clientManager);
    }

    private void testSendNotificationWithInvalidToken(String token, String testName) throws InterruptedException {
        String taskId = testName;
        Task taskData = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", token);
        
        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        when(clientManager.getOrCreate(any())).thenReturn(testHttpClient);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(taskData);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");
        
        // Verify the task was sent via HTTP
        assertEquals(1, testHttpClient.tasks.size());
        Task sentTask = testHttpClient.tasks.get(0);
        assertEquals(taskData.getId(), sentTask.getId());
        
        // Verify that no authentication header was sent (invalid token should not add header)
        assertEquals(1, testHttpClient.headers.size());
        Map<String, String> sentHeaders = testHttpClient.headers.get(0);
        assertTrue(sentHeaders.isEmpty(), "No headers should be sent when token is invalid");
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
    public void testSendNotificationSuccess() throws InterruptedException {
        String taskId = "task_send_success";
        Task taskData = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);
        
        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        when(clientManager.getOrCreate(any())).thenReturn(testHttpClient);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(taskData);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");
        
        // Verify the task was sent via HTTP
        assertEquals(1, testHttpClient.tasks.size());
        Task sentTask = testHttpClient.tasks.get(0);
        assertEquals(taskData.getId(), sentTask.getId());
        assertEquals(taskData.getContextId(), sentTask.getContextId());
        assertEquals(taskData.getStatus().state(), sentTask.getStatus().state());
    }

    @Test
    public void testSendNotificationWithTokenSuccess() throws InterruptedException {
        String taskId = "task_send_with_token";
        Task taskData = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");
        
        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        when(clientManager.getOrCreate(any())).thenReturn(testHttpClient);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(taskData);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");
        
        // Verify the task was sent via HTTP
        assertEquals(1, testHttpClient.tasks.size());
        Task sentTask = testHttpClient.tasks.get(0);
        assertEquals(taskData.getId(), sentTask.getId());
        
        // Verify that the X-A2A-Notification-Token header is sent with the correct token
        assertEquals(1, testHttpClient.headers.size());
        Map<String, String> sentHeaders = testHttpClient.headers.get(0);
        assertTrue(sentHeaders.containsKey(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        assertEquals(config.token(), sentHeaders.get(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
    }

    @Test
    public void testSendNotificationNoConfig() {
        String taskId = "task_send_no_config";
        Task taskData = createSampleTask(taskId, TaskState.COMPLETED);
        
        // Don't set any configuration in the store
        sender.sendNotification(taskData);

        // Verify no HTTP calls were made
        assertEquals(0, testHttpClient.tasks.size());
    }

    @Test
    public void testSendNotificationWithEmptyToken() throws InterruptedException {
        testSendNotificationWithInvalidToken("", "task_send_empty_token");
    }

    @Test
    public void testSendNotificationWithBlankToken() throws InterruptedException {
        testSendNotificationWithInvalidToken("   ", "task_send_blank_token");
    }

    @Test
    public void testSendNotificationMultipleConfigs() throws InterruptedException {
        String taskId = "task_multiple_configs";
        Task taskData = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config1 = createSamplePushConfig("http://notify.me/cfg1", "cfg1", null);
        PushNotificationConfig config2 = createSamplePushConfig("http://notify.me/cfg2", "cfg2", null);
        
        // Set up multiple configurations in the store
        configStore.setInfo(taskId, config1);
        configStore.setInfo(taskId, config2);

        TestHttpClient httpClient = spy(testHttpClient);
        when(clientManager.getOrCreate(any())).thenReturn(httpClient);

        // Set up latch to wait for async completion (2 calls expected)
        httpClient.latch = new CountDownLatch(2);

        sender.sendNotification(taskData);

        // Wait for the async operations to complete
        assertTrue(httpClient.latch.await(5, TimeUnit.SECONDS), "HTTP calls should complete within 5 seconds");
        
        // Verify both tasks were sent via HTTP
        assertEquals(2, httpClient.tasks.size());
        //assertEquals(2, testHttpClient.urls.size());
        verify(httpClient).post("/cfg1");
        verify(httpClient).post("/cfg2");
        // assertTrue(testHttpClient.urls.containsAll(java.util.List.of("http://notify.me/cfg1", "http://notify.me/cfg2")));
        
        // Both tasks should be identical (same task sent to different endpoints)
        for (Task sentTask : httpClient.tasks) {
            assertEquals(taskData.getId(), sentTask.getId());
            assertEquals(taskData.getContextId(), sentTask.getContextId());
            assertEquals(taskData.getStatus().state(), sentTask.getStatus().state());
        }
    }

    @Test
    public void testSendNotificationHttpError() {
        String taskId = "task_send_http_err";
        Task taskData = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/http_error", "cfg1", null);
        
        // Set up the configuration in the store
        configStore.setInfo(taskId, config);
        
        // Configure the test client to throw an exception
        testHttpClient.shouldThrowException = true;

        // This should not throw an exception - errors should be handled gracefully
        sender.sendNotification(taskData);

        // Verify no tasks were successfully processed due to the error
        assertEquals(0, testHttpClient.tasks.size());
    }
}
