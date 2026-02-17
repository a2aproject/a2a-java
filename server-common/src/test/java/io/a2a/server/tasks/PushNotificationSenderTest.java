package io.a2a.server.tasks;

import static io.a2a.client.http.A2AHttpClient.APPLICATION_JSON;
import static io.a2a.client.http.A2AHttpClient.CONTENT_TYPE;
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

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpResponse;
import io.a2a.common.A2AHeaders;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PushNotificationSenderTest {

    private TestHttpClient testHttpClient;
    private InMemoryPushNotificationConfigStore configStore;
    private BasePushNotificationSender sender;

    /**
     * Simple test implementation of A2AHttpClient that captures HTTP calls for verification.
     * Now captures StreamingEventKind events wrapped in StreamResponse format.
     */
    private static class TestHttpClient implements A2AHttpClient {
        final List<StreamingEventKind> events = Collections.synchronizedList(new ArrayList<>());
        final List<String> urls = Collections.synchronizedList(new ArrayList<>());
        final List<Map<String, String>> headers = Collections.synchronizedList(new ArrayList<>());
        final List<String> rawBodies = Collections.synchronizedList(new ArrayList<>());
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
                    // Store raw body for verification
                    rawBodies.add(body);

                    // Parse StreamResponse format to extract the event
                    // The body contains a wrapper with one of: task, message, statusUpdate, artifactUpdate
                    StreamingEventKind event = JsonUtil.fromJson(body, StreamingEventKind.class);
                    events.add(event);
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
                } catch (JsonProcessingException e) {
                    throw new IOException("Failed to parse StreamingEventKind JSON", e);
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
        Task taskData = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", token);
        
        // Set up the configuration in the store
        configStore.setInfo(taskId, config);
        
        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(taskData);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the task was sent via HTTP wrapped in StreamResponse format
        assertEquals(1, testHttpClient.events.size());
        StreamingEventKind sentEvent = testHttpClient.events.get(0);
        assertTrue(sentEvent instanceof Task, "Event should be a Task");
        Task sentTask = (Task) sentEvent;
        assertEquals(taskData.id(), sentTask.id());

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
    public void testSendNotificationSuccess() throws InterruptedException {
        String taskId = "task_send_success";
        Task taskData = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(taskData);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the task was sent via HTTP wrapped in StreamResponse format
        assertEquals(1, testHttpClient.events.size());
        StreamingEventKind sentEvent = testHttpClient.events.get(0);
        assertTrue(sentEvent instanceof Task, "Event should be a Task");
        Task sentTask = (Task) sentEvent;
        assertEquals(taskData.id(), sentTask.id());
        assertEquals(taskData.contextId(), sentTask.contextId());
        assertEquals(taskData.status().state(), sentTask.status().state());

        // Verify StreamResponse wrapper is present in raw body
        String rawBody = testHttpClient.rawBodies.get(0);
        assertTrue(rawBody.contains("\"task\""), "Raw body should contain 'task' discriminator for StreamResponse");
    }

    @Test
    public void testSendNotificationWithTokenSuccess() throws InterruptedException {
        String taskId = "task_send_with_token";
        Task taskData = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", "unique_token");

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(taskData);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the task was sent via HTTP wrapped in StreamResponse format
        assertEquals(1, testHttpClient.events.size());
        StreamingEventKind sentEvent = testHttpClient.events.get(0);
        assertTrue(sentEvent instanceof Task, "Event should be a Task");
        Task sentTask = (Task) sentEvent;
        assertEquals(taskData.id(), sentTask.id());

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
        Task taskData = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);

        // Don't set any configuration in the store
        sender.sendNotification(taskData);

        // Verify no HTTP calls were made
        assertEquals(0, testHttpClient.events.size());
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
        Task taskData = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);
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

        // Verify both events were sent via HTTP wrapped in StreamResponse format
        assertEquals(2, testHttpClient.events.size());
        assertEquals(2, testHttpClient.urls.size());
        assertTrue(testHttpClient.urls.containsAll(java.util.List.of("http://notify.me/cfg1", "http://notify.me/cfg2")));

        // Both events should be identical (same event sent to different endpoints)
        for (StreamingEventKind sentEvent : testHttpClient.events) {
            assertTrue(sentEvent instanceof Task, "Event should be a Task");
            Task sentTask = (Task) sentEvent;
            assertEquals(taskData.id(), sentTask.id());
            assertEquals(taskData.contextId(), sentTask.contextId());
            assertEquals(taskData.status().state(), sentTask.status().state());
        }
    }

    @Test
    public void testSendNotificationHttpError() {
        String taskId = "task_send_http_err";
        Task taskData = createSampleTask(taskId, TaskState.TASK_STATE_COMPLETED);
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/http_error", "cfg1", null);

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Configure the test client to throw an exception
        testHttpClient.shouldThrowException = true;

        // This should not throw an exception - errors should be handled gracefully
        sender.sendNotification(taskData);

        // Verify no events were successfully processed due to the error
        assertEquals(0, testHttpClient.events.size());
    }

    @Test
    public void testSendNotificationMessage() throws InterruptedException {
        String taskId = "task_send_message";
        Message message = Message.builder()
                .taskId(taskId)
                .role(Message.Role.ROLE_AGENT)
                .parts(new TextPart("Hello from agent"))
                .build();
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(message);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the message was sent via HTTP wrapped in StreamResponse format
        assertEquals(1, testHttpClient.events.size());
        StreamingEventKind sentEvent = testHttpClient.events.get(0);
        assertTrue(sentEvent instanceof Message, "Event should be a Message");
        Message sentMessage = (Message) sentEvent;
        assertEquals(taskId, sentMessage.taskId());

        // Verify StreamResponse wrapper with 'message' discriminator
        String rawBody = testHttpClient.rawBodies.get(0);
        assertTrue(rawBody.contains("\"message\""), "Raw body should contain 'message' discriminator for StreamResponse");
    }

    @Test
    public void testSendNotificationTaskStatusUpdate() throws InterruptedException {
        String taskId = "task_send_status_update";
        TaskStatusUpdateEvent statusUpdate = TaskStatusUpdateEvent.builder()
                .taskId(taskId)
                .contextId("ctx456")
                .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
                .build();
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(statusUpdate);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the status update was sent via HTTP wrapped in StreamResponse format
        assertEquals(1, testHttpClient.events.size());
        StreamingEventKind sentEvent = testHttpClient.events.get(0);
        assertTrue(sentEvent instanceof TaskStatusUpdateEvent, "Event should be a TaskStatusUpdateEvent");
        TaskStatusUpdateEvent sentUpdate = (TaskStatusUpdateEvent) sentEvent;
        assertEquals(taskId, sentUpdate.taskId());
        assertEquals(TaskState.TASK_STATE_WORKING, sentUpdate.status().state());

        // Verify StreamResponse wrapper with 'statusUpdate' discriminator
        String rawBody = testHttpClient.rawBodies.get(0);
        assertTrue(rawBody.contains("\"statusUpdate\""), "Raw body should contain 'statusUpdate' discriminator for StreamResponse");
    }

    @Test
    public void testSendNotificationTaskArtifactUpdate() throws InterruptedException {
        String taskId = "task_send_artifact_update";
        Artifact artifact = Artifact.builder()
                .artifactId("artifact-1")
                .name("test-artifact")
                .parts(Collections.singletonList(new TextPart("Artifact chunk")))
                .build();
        TaskArtifactUpdateEvent artifactUpdate = TaskArtifactUpdateEvent.builder()
                .taskId(taskId)
                .contextId("ctx456")
                .artifact(artifact)
                .build();
        PushNotificationConfig config = createSamplePushConfig("http://notify.me/here", "cfg1", null);

        // Set up the configuration in the store
        configStore.setInfo(taskId, config);

        // Set up latch to wait for async completion
        testHttpClient.latch = new CountDownLatch(1);

        sender.sendNotification(artifactUpdate);

        // Wait for the async operation to complete
        assertTrue(testHttpClient.latch.await(5, TimeUnit.SECONDS), "HTTP call should complete within 5 seconds");

        // Verify the artifact update was sent via HTTP wrapped in StreamResponse format
        assertEquals(1, testHttpClient.events.size());
        StreamingEventKind sentEvent = testHttpClient.events.get(0);
        assertTrue(sentEvent instanceof TaskArtifactUpdateEvent, "Event should be a TaskArtifactUpdateEvent");
        TaskArtifactUpdateEvent sentUpdate = (TaskArtifactUpdateEvent) sentEvent;
        assertEquals(taskId, sentUpdate.taskId());

        // Verify StreamResponse wrapper with 'artifactUpdate' discriminator
        String rawBody = testHttpClient.rawBodies.get(0);
        assertTrue(rawBody.contains("\"artifactUpdate\""), "Raw body should contain 'artifactUpdate' discriminator for StreamResponse");
    }
}
