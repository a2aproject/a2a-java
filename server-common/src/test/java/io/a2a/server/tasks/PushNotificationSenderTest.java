package io.a2a.server.tasks;

import static io.a2a.client.http.A2AHttpClient.APPLICATION_JSON;
import static io.a2a.client.http.A2AHttpClient.CONTENT_TYPE;
import static io.a2a.spec.Message.MESSAGE;
import static io.a2a.spec.Task.TASK;
import static io.a2a.spec.TaskArtifactUpdateEvent.ARTIFACT_UPDATE;
import static io.a2a.spec.TaskState.WORKING;
import static io.a2a.spec.TaskStatusUpdateEvent.STATUS_UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.common.A2AHeaders;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UpdateEvent;
import io.a2a.util.Utils;

public class PushNotificationSenderTest {

    private TestHttpClient testHttpClient;
    private InMemoryPushNotificationConfigStore configStore;
    private BasePushNotificationSender sender;

    /**
     * Simple test implementation of A2AHttpClient that captures HTTP calls for verification
     */
    private static class TestHttpClient implements A2AHttpClient {
        final List<Task> tasks = Collections.synchronizedList(new ArrayList<>());
        final List<Message> messages = Collections.synchronizedList(new ArrayList<>());
        final List<TaskStatusUpdateEvent> statusUpdateEvents = Collections.synchronizedList(new ArrayList<>());
        final List<TaskArtifactUpdateEvent> artifactUpdateEvents = Collections.synchronizedList(new ArrayList<>());
        final List<String> urls = Collections.synchronizedList(new ArrayList<>());
        final List<Map<String, String>> headers = Collections.synchronizedList(new ArrayList<>());
        volatile CountDownLatch latch;
        volatile boolean shouldThrowException = false;

        @Override
        public GetBuilder createGet() {
            return null;
        }

        @Override
        public PostBuilder createPost() {
            return new TestPostBuilder();
        }

        @Override
        public DeleteBuilder createDelete() {
            return null;
        }

        class TestPostBuilder implements A2AHttpClient.PostBuilder {
            private volatile String body;
            private volatile String url;
            private final Map<String, String> requestHeaders = new java.util.HashMap<>();

            @Override
            public PostBuilder body(String body) {
                this.body = body;
                return this;
            }

            @Override
            public A2AHttpResponse post() throws IOException, InterruptedException {
                if (shouldThrowException) {
                    throw new IOException("Simulated network error");
                }
                
                try {
                    StreamingEventKind kind = Utils.unmarshalStreamingEventKindFrom(body);
                    switch (kind.getKind()) {
                        case TASK:
                            tasks.add((Task) kind);
                            break;
                        case MESSAGE:
                            messages.add((Message) kind);
                            break;
                        case STATUS_UPDATE:
                            statusUpdateEvents.add((TaskStatusUpdateEvent)kind);
                            break;
                        case ARTIFACT_UPDATE:
                            artifactUpdateEvents.add((TaskArtifactUpdateEvent) kind);
                            break;
                    }
                    urls.add(url);
                    headers.add(new java.util.HashMap<>(requestHeaders));
                    
                    return new A2AHttpResponse() {
                        @Override
                        public int status() {
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
                    };
                } finally {
                    if (latch != null) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public CompletableFuture<Void> postAsyncSSE(Consumer<String> messageConsumer, Consumer<Throwable> errorConsumer, Runnable completeRunnable) throws IOException, InterruptedException {
                return null;
            }

            @Override
            public PostBuilder url(String url) {
                this.url = url;
                return this;
            }

            @Override
            public PostBuilder addHeader(String name, String value) {
                requestHeaders.put(name, value);
                return this;
            }

            @Override
            public PostBuilder addHeaders(Map<String, String> headers) {
                requestHeaders.putAll(headers);
                return this;
            }
        }
    }

    @BeforeEach
    public void setUp() {
        testHttpClient = new TestHttpClient();
        configStore = new InMemoryPushNotificationConfigStore();
        sender = new BasePushNotificationSender(configStore, testHttpClient);
    }

    private void testSendNotificationWithInvalidToken(String token, String testName) throws InterruptedException {
        String taskId = testName;
        Task taskData = createSampleTask(taskId, TaskState.COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", token);
        
        // Set up the configuration in the store
        configStore.setInfo(taskId, config);
        
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
        assertEquals(1, sentHeaders.size());
        assertFalse(sentHeaders.containsKey(A2AHeaders.X_A2A_NOTIFICATION_TOKEN),
                "X-A2A-Notification-Token header should not be sent when token is invalid");
        // Content-Type header should always be present
        assertTrue(sentHeaders.containsKey(CONTENT_TYPE));
        assertEquals(APPLICATION_JSON, sentHeaders.get(CONTENT_TYPE));
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
        assertEquals(2, sentHeaders.size());
        assertTrue(sentHeaders.containsKey(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        assertEquals(config.token(), sentHeaders.get(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        // Content-Type header should always be present
        assertTrue(sentHeaders.containsKey(CONTENT_TYPE));
        assertEquals(APPLICATION_JSON, sentHeaders.get(CONTENT_TYPE));

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
        
        // Set up latch to wait for async completion (2 calls expected)
        testHttpClient.latch = new CountDownLatch(2);

        sender.sendNotification(taskData);

        // Wait for the async operations to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP calls should complete within 5 seconds");
        
        // Verify both tasks were sent via HTTP
        assertEquals(2, testHttpClient.tasks.size());
        assertEquals(2, testHttpClient.urls.size());
        assertTrue(testHttpClient.urls.containsAll(java.util.List.of("http://notify.me/cfg1", "http://notify.me/cfg2")));
        
        // Both tasks should be identical (same task sent to different endpoints)
        for (Task sentTask : testHttpClient.tasks) {
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

    @Test
    public void testSendNotificationWithMessage() throws InterruptedException {
        String taskId = "task_send_notification_with_message";
        String messageId = taskId + "msg-0";
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        Message message = new Message.Builder()
                .taskId(taskId)
                .messageId(messageId)
                .role(Message.Role.USER)
                .parts(List.of(new TextPart("This is a message sent as a push notification")))
                .build();

        sender.sendNotification(message);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the message was sent via HTTP
        assertEquals(0, testHttpClient.tasks.size());
        assertEquals(1, testHttpClient.messages.size());

        Message sentMessage = testHttpClient.messages.get(0);
        assertEquals(taskId, sentMessage.getTaskId());
        assertEquals(messageId, sentMessage.getMessageId());

        // Verify that the X-A2A-Notification-Token header is sent with the correct token
        assertEquals(1, testHttpClient.headers.size());
        Map<String, String> sentHeaders = testHttpClient.headers.get(0);
        assertEquals(2, sentHeaders.size());
        assertTrue(sentHeaders.containsKey(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        assertEquals(config.token(), sentHeaders.get(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        // Content-Type header should always be present
        assertTrue(sentHeaders.containsKey(CONTENT_TYPE));
        assertEquals(APPLICATION_JSON, sentHeaders.get(CONTENT_TYPE));
    }

    @Test
    public void testSendNotificationWithStatusUpdateEvent() throws InterruptedException {
        String taskId = "task_send_notification_with_status_update_event";
        String contextId = taskId + "-context";

        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
                .taskId(taskId)
                .status(new TaskStatus(WORKING))
                .contextId(contextId)
                .build();

        sender.sendNotification(event);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the event was sent via HTTP
        assertEquals(1, testHttpClient.statusUpdateEvents.size());

        TaskStatusUpdateEvent updateEvent = testHttpClient.statusUpdateEvents.get(0);
        assertEquals(taskId, updateEvent.getTaskId());
        assertEquals(contextId, updateEvent.getContextId());
        assertEquals(WORKING, updateEvent.getStatus().state());

        // Verify that the X-A2A-Notification-Token header is sent with the correct token
        assertEquals(1, testHttpClient.headers.size());
        Map<String, String> sentHeaders = testHttpClient.headers.get(0);
        assertEquals(2, sentHeaders.size());
        assertTrue(sentHeaders.containsKey(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        assertEquals(config.token(), sentHeaders.get(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        // Content-Type header should always be present
        assertTrue(sentHeaders.containsKey(CONTENT_TYPE));
        assertEquals(APPLICATION_JSON, sentHeaders.get(CONTENT_TYPE));
    }

    @Test
    public void testSendNotificationWithArtifactUpdateEvent() throws InterruptedException {
        String taskId = "task_send_notification_with_artifact_update_event";
        String contextId = taskId + "-context";
        String artifactId = taskId + "-artifact";

        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
                .taskId(taskId)
                .contextId(contextId)
                .artifact(new Artifact.Builder()
                        .artifactId(artifactId)
                        .parts(List.of(new TextPart("This is a part of the artifact that is updated")))
                        .build())
                .lastChunk(false)
                .build();

        sender.sendNotification(event);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the event was sent via HTTP
        assertEquals(1, testHttpClient.artifactUpdateEvents.size());

        TaskArtifactUpdateEvent updateEvent = testHttpClient.artifactUpdateEvents.get(0);
        assertEquals(taskId, updateEvent.getTaskId());
        assertEquals(contextId, updateEvent.getContextId());
        assertEquals(artifactId, updateEvent.getArtifact().artifactId());
        assertFalse(updateEvent.isLastChunk());

        // Verify that the X-A2A-Notification-Token header is sent with the correct token
        assertEquals(1, testHttpClient.headers.size());
        Map<String, String> sentHeaders = testHttpClient.headers.get(0);
        assertEquals(2, sentHeaders.size());
        assertTrue(sentHeaders.containsKey(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        assertEquals(config.token(), sentHeaders.get(A2AHeaders.X_A2A_NOTIFICATION_TOKEN));
        // Content-Type header should always be present
        assertTrue(sentHeaders.containsKey(CONTENT_TYPE));
        assertEquals(APPLICATION_JSON, sentHeaders.get(CONTENT_TYPE));
    }
}
