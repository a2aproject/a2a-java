package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        Task testTask = new io.a2a.spec.Task.Builder()
            .id("direct-test-task")
            .contextId("test-context")
            .status(new io.a2a.spec.TaskStatus(io.a2a.spec.TaskState.SUBMITTED))
            .build();

        // Directly trigger the mock
        mockPushNotificationSender.sendNotification(testTask);

        // Verify it was captured
        Queue<Task> captured = mockPushNotificationSender.getCapturedEvents();
        assertEquals(1, captured.size());
        assertEquals("direct-test-task", captured.peek().getId());
    }

    @Test
    public void testJpaDatabasePushNotificationConfigStoreIntegration() throws Exception {
        final String taskId = "push-notify-test-" + System.currentTimeMillis();
        final String contextId = "test-context";

        // Step 1: Create the task
        Message createMessage = new Message.Builder()
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
        PushNotificationConfig pushConfig = new PushNotificationConfig.Builder()
            .url("http://localhost:9999/mock-endpoint")
            .token("test-token-123")
            .id("test-config-1")
            .build();

        TaskPushNotificationConfig taskPushConfig = new TaskPushNotificationConfig(taskId, pushConfig);
        TaskPushNotificationConfig setResult = client.setTaskPushNotificationConfiguration(taskPushConfig);
        assertNotNull(setResult);

        // Step 3: Verify the configuration was stored using client API
        TaskPushNotificationConfig storedConfig = client.getTaskPushNotificationConfiguration(
            new GetTaskPushNotificationConfigParams(taskId));

        assertNotNull(storedConfig);
        assertEquals(taskId, storedConfig.taskId());
        assertEquals("test-config-1", storedConfig.pushNotificationConfig().id());
        assertEquals("http://localhost:9999/mock-endpoint", storedConfig.pushNotificationConfig().url());
        assertEquals("test-token-123", storedConfig.pushNotificationConfig().token());

        // Step 4: Update the task to trigger the notification
        Message updateMessage = new Message.Builder()
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
        Queue<Task> capturedTasks = mockPushNotificationSender.getCapturedEvents();

        // Verify the notification contains the correct task with artifacts
        Task notifiedTaskWithArtifact = capturedTasks.stream()
            .filter(t -> taskId.equals(t.getId()) && t.getArtifacts() != null && t.getArtifacts().size() > 0)
            .findFirst()
            .orElse(null);

        assertNotNull(notifiedTaskWithArtifact, "Notification should contain the updated task with artifacts");
        assertEquals(taskId, notifiedTaskWithArtifact.getId());
        assertEquals(1, notifiedTaskWithArtifact.getArtifacts().size(), "Task should have one artifact from the update");

        // Step 7: Clean up - delete the push notification configuration
        client.deleteTaskPushNotificationConfigurations(
            new DeleteTaskPushNotificationConfigParams(taskId, "test-config-1"));

        // Verify deletion by asserting that getting the config now throws an exception
        assertThrows(A2AClientException.class, () -> {
            client.getTaskPushNotificationConfiguration(new GetTaskPushNotificationConfigParams(taskId));
        }, "Getting a deleted config should throw an A2AClientException");
    }
}
