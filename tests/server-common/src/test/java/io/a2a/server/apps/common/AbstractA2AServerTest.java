package io.a2a.server.apps.common;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

import io.a2a.client.Client;
import io.a2a.client.ClientBuilder;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskUpdateEvent;
import jakarta.ws.rs.core.MediaType;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Artifact;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.Event;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.TransportProtocol;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * This test requires doing some work on the server to add/get/delete tasks, and enqueue events. This is exposed via REST,
 * which delegates to {@link TestUtilsBean}.
 */
public abstract class AbstractA2AServerTest {

    protected static final Task MINIMAL_TASK = new Task.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task CANCEL_TASK = new Task.Builder()
            .id("cancel-task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task CANCEL_TASK_NOT_SUPPORTED = new Task.Builder()
            .id("cancel-task-not-supported-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task SEND_MESSAGE_NOT_SUPPORTED = new Task.Builder()
            .id("task-not-supported-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    protected static final Message MESSAGE = new Message.Builder()
            .messageId("111")
            .role(Message.Role.AGENT)
            .parts(new TextPart("test message"))
            .build();
    public static final String APPLICATION_JSON = "application/json";

    protected final int serverPort;
    private Client client;
    private Client nonStreamingClient;

    protected AbstractA2AServerTest(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Get the transport protocol to use for this test (e.g., "JSONRPC", "GRPC").
     */
    protected abstract String getTransportProtocol();

    /**
     * Get the transport URL for this test.
     */
    protected abstract String getTransportUrl();

    /**
     * Get the transport configs to use for this test.
     */
    protected abstract void configureTransport(ClientBuilder builder);

    @Test
    public void testTaskStoreMethodsSanityTest() throws Exception {
        Task task = new Task.Builder(MINIMAL_TASK).id("abcde").build();
        saveTaskInTaskStore(task);
        Task saved = getTaskFromTaskStore(task.getId());
        assertEquals(task.getId(), saved.getId());
        assertEquals(task.getContextId(), saved.getContextId());
        assertEquals(task.getStatus().state(), saved.getStatus().state());

        deleteTaskInTaskStore(task.getId());
        Task saved2 = getTaskFromTaskStore(task.getId());
        assertNull(saved2);
    }

    @Test
    public void testGetTaskSuccess() throws Exception {
        testGetTask();
    }

    private void testGetTask() throws Exception {
        testGetTask(null);
    }

    private void testGetTask(String mediaType) throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            Task response = getClient().getTask(new TaskQueryParams(MINIMAL_TASK.getId()));
            assertEquals("task-123", response.getId());
            assertEquals("session-xyz", response.getContextId());
            assertEquals(TaskState.SUBMITTED, response.getStatus().state());
        } catch (A2AClientException e) {
            fail("Unexpected exception during getTask: " + e.getMessage(), e);
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetTaskNotFound() throws Exception {
        assertTrue(getTaskFromTaskStore("non-existent-task") == null);
        try {
            getClient().getTask(new TaskQueryParams("non-existent-task"));
            fail("Expected A2AClientException for non-existent task");
        } catch (A2AClientException e) {
            // Expected - the client should throw an exception for non-existent tasks
            assertInstanceOf(TaskNotFoundError.class, e.getCause());
        }
    }

    @Test
    public void testCancelTaskSuccess() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK);
        try {
            Task task = getClient().cancelTask(new TaskIdParams(CANCEL_TASK.getId()));
            assertEquals(CANCEL_TASK.getId(), task.getId());
            assertEquals(CANCEL_TASK.getContextId(), task.getContextId());
            assertEquals(TaskState.CANCELED, task.getStatus().state());
        } catch (A2AClientException e) {
            fail("Unexpected exception during cancel task: " + e.getMessage(), e);
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK.getId());
        }
    }

    @Test
    public void testCancelTaskNotSupported() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED);
        try {
            getClient().cancelTask(new TaskIdParams(CANCEL_TASK_NOT_SUPPORTED.getId()));
            fail("Expected A2AClientException for unsupported cancel operation");
        } catch (A2AClientException e) {
            // Expected - the client should throw an exception for unsupported operations
            assertInstanceOf(UnsupportedOperationError.class, e.getCause());
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED.getId());
        }
    }

    @Test
    public void testCancelTaskNotFound() {
        try {
            getClient().cancelTask(new TaskIdParams("non-existent-task"));
            fail("Expected A2AClientException for non-existent task");
        } catch (A2AClientException e) {
            // Expected - the client should throw an exception for non-existent tasks
            assertInstanceOf(TaskNotFoundError.class, e.getCause());
        }
    }

    @Test
    public void testSendMessageNewMessageSuccess() throws Exception {
        assertTrue(getTaskFromTaskStore(MINIMAL_TASK.getId()) == null);
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();


        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Message> receivedMessage = new AtomicReference<>();
        AtomicBoolean wasUnexpectedEvent = new AtomicBoolean(false);
        BiConsumer<ClientEvent, AgentCard> consumer = (event, agentCard) -> {
            if (event instanceof MessageEvent messageEvent) {
                if (latch.getCount() > 0) {
                    receivedMessage.set(messageEvent.getMessage());
                    latch.countDown();
                } else {
                    wasUnexpectedEvent.set(true);
                }
            } else {
                wasUnexpectedEvent.set(true);
            }
        };

        // testing the non-streaming send message
        getNonStreamingClient().sendMessage(message, List.of(consumer), null);

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertFalse(wasUnexpectedEvent.get());
        Message messageResponse = receivedMessage.get();
        assertNotNull(messageResponse);
        assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
        assertEquals(MESSAGE.getRole(), messageResponse.getRole());
        Part<?> part = messageResponse.getParts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("test message", ((TextPart) part).getText());
    }

    @Test
    public void testSendMessageExistingTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Message> receivedMessage = new AtomicReference<>();
            AtomicBoolean wasUnexpectedEvent = new AtomicBoolean(false);
            BiConsumer<ClientEvent, AgentCard> consumer = (event, agentCard) -> {
                if (event instanceof MessageEvent messageEvent) {
                    if (latch.getCount() > 0) {
                        receivedMessage.set(messageEvent.getMessage());
                        latch.countDown();
                    } else {
                        wasUnexpectedEvent.set(true);
                    }
                } else {
                    wasUnexpectedEvent.set(true);
                }
            };

            // testing the non-streaming send message
            getNonStreamingClient().sendMessage(message, List.of(consumer), null);
            assertFalse(wasUnexpectedEvent.get());
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            Message messageResponse = receivedMessage.get();
            assertNotNull(messageResponse);
            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
            Part<?> part = messageResponse.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());
        } catch (A2AClientException e) {
            fail("Unexpected exception during sendMessage: " + e.getMessage(), e);
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testSetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig taskPushConfig =
                    new TaskPushNotificationConfig(
                            MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());
            TaskPushNotificationConfig config = getClient().setTaskPushNotificationConfiguration(taskPushConfig);
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (A2AClientException e) {
            fail("Unexpected exception during set push notification test: " + e.getMessage(), e);
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig taskPushConfig =
                    new TaskPushNotificationConfig(
                            MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());

            TaskPushNotificationConfig setResult = getClient().setTaskPushNotificationConfiguration(taskPushConfig);
            assertNotNull(setResult);

            TaskPushNotificationConfig config = getClient().getTaskPushNotificationConfiguration(
                    new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (A2AClientException e) {
            fail("Unexpected exception during get push notification test: " + e.getMessage(), e);
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testError() throws A2AClientException {
        Message message = new Message.Builder(MESSAGE)
                .taskId(SEND_MESSAGE_NOT_SUPPORTED.getId())
                .contextId(SEND_MESSAGE_NOT_SUPPORTED.getContextId())
                .build();

        try {
            getNonStreamingClient().sendMessage(message);

            // For non-streaming clients, the error should still be thrown as an exception
            fail("Expected A2AClientException for unsupported send message operation");
        } catch (A2AClientException e) {
            // Expected - the client should throw an exception for unsupported operations
            assertInstanceOf(UnsupportedOperationError.class, e.getCause());
        }
    }

    @Test
    public void testGetAgentCard() throws A2AClientException {
        AgentCard agentCard = getClient().getAgentCard();
        assertNotNull(agentCard);
        assertEquals("test-card", agentCard.name());
        assertEquals("A test agent card", agentCard.description());
        assertEquals(getTransportUrl(), agentCard.url());
        assertEquals("1.0", agentCard.version());
        assertEquals("http://example.com/docs", agentCard.documentationUrl());
        assertTrue(agentCard.capabilities().pushNotifications());
        assertTrue(agentCard.capabilities().streaming());
        assertTrue(agentCard.capabilities().stateTransitionHistory());
        assertTrue(agentCard.skills().isEmpty());
        assertFalse(agentCard.supportsAuthenticatedExtendedCard());
    }

    @Test
    public void testSendMessageStreamNewMessageSuccess() throws Exception {
        testSendStreamingMessage(false);
    }

    @Test
    public void testSendMessageStreamExistingTaskSuccess() throws Exception {
        testSendStreamingMessage(true);
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void testResubscribeExistingTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // attempting to send a streaming message instead of explicitly calling queueManager#createOrTap
            // does not work because after the message is sent, the queue becomes null but task resubscription
            // requires the queue to still be active
            ensureQueueForTask(MINIMAL_TASK.getId());

            CountDownLatch eventLatch = new CountDownLatch(2);
            AtomicReference<TaskArtifactUpdateEvent> artifactUpdateEvent = new AtomicReference<>();
            AtomicReference<TaskStatusUpdateEvent> statusUpdateEvent = new AtomicReference<>();
            AtomicBoolean wasUnexpectedEvent = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            // Create consumer to handle resubscribed events
            BiConsumer<ClientEvent, AgentCard> consumer = (event, agentCard) -> {
                if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                    if (taskUpdateEvent.getUpdateEvent() instanceof TaskArtifactUpdateEvent artifactEvent) {
                        artifactUpdateEvent.set(artifactEvent);
                        eventLatch.countDown();
                    } else if (taskUpdateEvent.getUpdateEvent() instanceof TaskStatusUpdateEvent statusEvent) {
                        statusUpdateEvent.set(statusEvent);
                        eventLatch.countDown();
                    } else {
                        wasUnexpectedEvent.set(true);
                    }
                } else {
                    wasUnexpectedEvent.set(true);
                }
            };

            // Create error handler
            Consumer<Throwable> errorHandler = error -> {
                if (!isStreamClosedError(error)) {
                    errorRef.set(error);
                }
                eventLatch.countDown();
            };

            // Count down when the streaming subscription is established
            CountDownLatch subscriptionLatch = new CountDownLatch(1);
            awaitStreamingSubscription()
                    .whenComplete((unused, throwable) -> subscriptionLatch.countDown());

            // Resubscribe to the task with specific consumer and error handler
            getClient().resubscribe(new TaskIdParams(MINIMAL_TASK.getId()), List.of(consumer), errorHandler);

            // Wait for subscription to be established
            assertTrue(subscriptionLatch.await(15, TimeUnit.SECONDS));

            // Enqueue events on the server
            List<Event> events = List.of(
                    new TaskArtifactUpdateEvent.Builder()
                            .taskId(MINIMAL_TASK.getId())
                            .contextId(MINIMAL_TASK.getContextId())
                            .artifact(new Artifact.Builder()
                                    .artifactId("11")
                                    .parts(new TextPart("text"))
                                    .build())
                            .build(),
                    new TaskStatusUpdateEvent.Builder()
                            .taskId(MINIMAL_TASK.getId())
                            .contextId(MINIMAL_TASK.getContextId())
                            .status(new TaskStatus(TaskState.COMPLETED))
                            .isFinal(true)
                            .build());

            for (Event event : events) {
                enqueueEventOnServer(event);
            }

            // Wait for events to be received
            assertTrue(eventLatch.await(30, TimeUnit.SECONDS));
            assertFalse(wasUnexpectedEvent.get());
            assertNull(errorRef.get());

            // Verify artifact update event
            TaskArtifactUpdateEvent receivedArtifactEvent = artifactUpdateEvent.get();
            assertNotNull(receivedArtifactEvent);
            assertEquals(MINIMAL_TASK.getId(), receivedArtifactEvent.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), receivedArtifactEvent.getContextId());
            Part<?> part = receivedArtifactEvent.getArtifact().parts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("text", ((TextPart) part).getText());

            // Verify status update event
            TaskStatusUpdateEvent receivedStatusEvent = statusUpdateEvent.get();
            assertNotNull(receivedStatusEvent);
            assertEquals(MINIMAL_TASK.getId(), receivedStatusEvent.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), receivedStatusEvent.getContextId());
            assertEquals(TaskState.COMPLETED, receivedStatusEvent.getStatus().state());
            assertNotNull(receivedStatusEvent.getStatus().timestamp());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testResubscribeNoExistingTaskError() throws Exception {
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Create error handler to capture the TaskNotFoundError
        Consumer<Throwable> errorHandler = error -> {
            if (!isStreamClosedError(error)) {
                errorRef.set(error);
            }
            errorLatch.countDown();
        };

        try {
            getClient().resubscribe(new TaskIdParams("non-existent-task"), List.of(), errorHandler);

            // Wait for error to be captured (may come via error handler for streaming)
            boolean errorReceived = errorLatch.await(10, TimeUnit.SECONDS);

            if (errorReceived) {
                // Error came via error handler
                Throwable error = errorRef.get();
                assertNotNull(error);
                if (error instanceof A2AClientException) {
                    assertInstanceOf(TaskNotFoundError.class, ((A2AClientException) error).getCause());
                } else {
                    // Check if it's directly a TaskNotFoundError or walk the cause chain
                    Throwable cause = error;
                    boolean foundTaskNotFound = false;
                    while (cause != null && !foundTaskNotFound) {
                        if (cause instanceof TaskNotFoundError) {
                            foundTaskNotFound = true;
                        }
                        cause = cause.getCause();
                    }
                    if (!foundTaskNotFound) {
                        fail("Expected TaskNotFoundError in error chain");
                    }
                }
            } else {
                fail("Expected error for non-existent task resubscription");
            }
        } catch (A2AClientException e) {
            fail("Expected error for non-existent task resubscription");
        }
    }

    @Test
    public void testListPushNotificationConfigWithConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config1")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config2")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);

        try {
            List<TaskPushNotificationConfig> result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
            assertEquals(2, result.size());
            assertEquals(new TaskPushNotificationConfig(MINIMAL_TASK.getId(), notificationConfig1), result.get(0));
            assertEquals(new TaskPushNotificationConfig(MINIMAL_TASK.getId(), notificationConfig2), result.get(1));
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testListPushNotificationConfigWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://1.example.com")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://2.example.com")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);

        // will overwrite the previous one
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);
        try {
            List<TaskPushNotificationConfig> result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
            assertEquals(1, result.size());

            PushNotificationConfig expectedNotificationConfig = new PushNotificationConfig.Builder()
                    .url("http://2.example.com")
                    .id(MINIMAL_TASK.getId())
                    .build();
            assertEquals(new TaskPushNotificationConfig(MINIMAL_TASK.getId(), expectedNotificationConfig),
                    result.get(0));
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testListPushNotificationConfigTaskNotFound() {
        try {
            List<TaskPushNotificationConfig> result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams("non-existent-task"));
            fail();
        } catch (A2AClientException e) {
            assertInstanceOf(TaskNotFoundError.class, e.getCause());
        }
    }

    @Test
    public void testListPushNotificationConfigEmptyList() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            List<TaskPushNotificationConfig> result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
            assertEquals(0, result.size());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithValidConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        saveTaskInTaskStore(new Task.Builder()
                .id("task-456")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build());

        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config1")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config2")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);
        savePushNotificationConfigInStore("task-456", notificationConfig1);

        try {
            // specify the config ID to delete
            getClient().deleteTaskPushNotificationConfigurations(
                    new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), "config1"));

            // should now be 1 left
            List<TaskPushNotificationConfig> result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
            assertEquals(1, result.size());

            // should remain unchanged, this is a different task
            result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams("task-456"));
            assertEquals(1, result.size());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deletePushNotificationConfigInStore("task-456", "config1");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
            deleteTaskInTaskStore("task-456");
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithNonExistingConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config1")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config2")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);

        try {
            getClient().deleteTaskPushNotificationConfigurations(
                    new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), "non-existent-config-id"));

            // should remain unchanged
            List<TaskPushNotificationConfig> result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
            assertEquals(2, result.size());
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testDeletePushNotificationConfigTaskNotFound() {
        try {
            getClient().deleteTaskPushNotificationConfigurations(
                    new DeleteTaskPushNotificationConfigParams("non-existent-task",
                            "non-existent-config-id"));
            fail();
        } catch (A2AClientException e) {
            assertInstanceOf(TaskNotFoundError.class, e.getCause());
        }
    }

    @Test
    public void testDeletePushNotificationConfigSetWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://1.example.com")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://2.example.com")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);

        // this one will overwrite the previous one
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);

        try {
            getClient().deleteTaskPushNotificationConfigurations(
                    new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.getId(), MINIMAL_TASK.getId()));

            // should now be 0
            List<TaskPushNotificationConfig> result = getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigParams(MINIMAL_TASK.getId()), null);
            assertEquals(0, result.size());
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testMalformedJSONRPCRequest() {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        
        // missing closing bracket
        String malformedRequest = "{\"jsonrpc\": \"2.0\", \"method\": \"message/send\", \"params\": {\"foo\": \"bar\"}";
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(malformedRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new JSONParseError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidParamsJSONRPCRequest() {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        
        String invalidParamsRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": "not_a_dict", "id": "1"}
            """;
        testInvalidParams(invalidParamsRequest);

        invalidParamsRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": {"message": {"parts": "invalid"}}, "id": "1"}
            """;
        testInvalidParams(invalidParamsRequest);
    }

    private void testInvalidParams(String invalidParamsRequest) {
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidParamsRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidParamsError().getCode(), response.getError().getCode());
        assertEquals("1", response.getId());
    }

    @Test
    public void testInvalidJSONRPCRequestMissingJsonrpc() {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        
        String invalidRequest = """
            {
             "method": "message/send",
             "params": {}
            }
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestMissingMethod() {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        
        String invalidRequest = """
            {"jsonrpc": "2.0", "params": {}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestInvalidId() {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        
        String invalidRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": {}, "id": {"bad": "type"}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestNonExistentMethod() {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        
        String invalidRequest = """
            {"jsonrpc": "2.0", "method" : "nonexistent/method", "params": {}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new MethodNotFoundError().getCode(), response.getError().getCode());
    }

    @Test
    public void testNonStreamingMethodWithAcceptHeader() throws Exception {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        testGetTask(MediaType.APPLICATION_JSON);
    }

    @Test
    public void testStreamingMethodWithAcceptHeader() throws Exception {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");
        
        testSendStreamingMessageWithHttpClient(MediaType.SERVER_SENT_EVENTS);
    }

    @Test
    public void testStreamingMethodWithoutAcceptHeader() throws Exception {
        // skip this test for non-JSONRPC transports
        assumeTrue(TransportProtocol.JSONRPC.asString().equals(getTransportProtocol()),
                "JSONRPC-specific test");

        testSendStreamingMessageWithHttpClient(null);
    }

    private void testSendStreamingMessageWithHttpClient(String mediaType) throws Exception {
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));

        CompletableFuture<HttpResponse<Stream<String>>> responseFuture = initialiseStreamingRequest(request, mediaType);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        responseFuture.thenAccept(response -> {
            if (response.statusCode() != 200) {
                //errorRef.set(new IllegalStateException("Status code was " + response.statusCode()));
                throw new IllegalStateException("Status code was " + response.statusCode());
            }
            response.body().forEach(line -> {
                try {
                    SendStreamingMessageResponse jsonResponse = extractJsonResponseFromSseLine(line);
                    if (jsonResponse != null) {
                        assertNull(jsonResponse.getError());
                        Message messageResponse =  (Message) jsonResponse.getResult();
                        assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
                        assertEquals(MESSAGE.getRole(), messageResponse.getRole());
                        Part<?> part = messageResponse.getParts().get(0);
                        assertEquals(Part.Kind.TEXT, part.getKind());
                        assertEquals("test message", ((TextPart) part).getText());
                        latch.countDown();
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }).exceptionally(t -> {
            if (!isStreamClosedError(t)) {
                errorRef.set(t);
            }
            latch.countDown();
            return null;
        });


        boolean dataRead = latch.await(20, TimeUnit.SECONDS);
        Assertions.assertTrue(dataRead);
        Assertions.assertNull(errorRef.get());

    }

    public void testSendStreamingMessage(boolean createTask) throws Exception {
        if (createTask) {
            saveTaskInTaskStore(MINIMAL_TASK);
        }
        try {
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Message> receivedMessage = new AtomicReference<>();
            AtomicBoolean wasUnexpectedEvent = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            BiConsumer<ClientEvent, AgentCard> consumer = (event, agentCard) -> {
                if (event instanceof MessageEvent messageEvent) {
                    if (latch.getCount() > 0) {
                        receivedMessage.set(messageEvent.getMessage());
                        latch.countDown();
                    } else {
                        wasUnexpectedEvent.set(true);
                    }
                } else {
                    wasUnexpectedEvent.set(true);
                }
            };

            Consumer<Throwable> errorHandler = error -> {
                errorRef.set(error);
                latch.countDown();
            };

            // testing the streaming send message
            getClient().sendMessage(message, List.of(consumer), errorHandler);

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertFalse(wasUnexpectedEvent.get());
            assertNull(errorRef.get());

            Message messageResponse = receivedMessage.get();
            assertNotNull(messageResponse);
            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
            Part<?> part = messageResponse.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());
        } catch (A2AClientException e) {
            fail("Unexpected exception during sendMessage: " + e.getMessage(), e);
        } finally {
            if (createTask) {
                deleteTaskInTaskStore(MINIMAL_TASK.getId());
            }
        }
    }

    private CompletableFuture<HttpResponse<Stream<String>>> initialiseStreamingRequest(
            StreamingJSONRPCRequest<?> request, String mediaType) throws Exception {

        // Create the client
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        // Create the request
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/"))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(request)))
                .header("Content-Type", APPLICATION_JSON);
        if (mediaType != null) {
            builder.header("Accept", mediaType);
        }
        HttpRequest httpRequest = builder.build();


        // Send request async and return the CompletableFuture
        return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines());
    }

    private SendStreamingMessageResponse extractJsonResponseFromSseLine(String line) throws JsonProcessingException {
        line = extractSseData(line);
        if (line != null) {
            return Utils.OBJECT_MAPPER.readValue(line, SendStreamingMessageResponse.class);
        }
        return null;
    }

    private static String extractSseData(String line) {
        if (line.startsWith("data:")) {
            line =  line.substring(5).trim();
            return line;
        }
        return null;
    }

    protected boolean isStreamClosedError(Throwable throwable) {
        // Unwrap the CompletionException
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof EOFException) {
                return true;
            }
            if (cause instanceof IOException && cause.getMessage() != null
                    && cause.getMessage().contains("cancelled")) {
                // stream is closed upon cancellation
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    protected void saveTaskInTaskStore(Task task) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task"))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(task)))
                .header("Content-Type", APPLICATION_JSON)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Saving task failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
    }

    protected Task getTaskFromTaskStore(String taskId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task/" + taskId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Getting task failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
        return Utils.OBJECT_MAPPER.readValue(response.body(), Task.TYPE_REFERENCE);
    }

    protected void deleteTaskInTaskStore(String taskId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(("http://localhost:" + serverPort + "/test/task/" + taskId)))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Deleting task failed!" + response.body());
        }
    }

    protected void ensureQueueForTask(String taskId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/queue/ensure/" + taskId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Ensuring queue failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
    }

    protected void enqueueEventOnServer(Event event) throws Exception {
        String path;
        if (event instanceof TaskArtifactUpdateEvent e) {
            path = "test/queue/enqueueTaskArtifactUpdateEvent/" + e.getTaskId();
        } else if (event instanceof TaskStatusUpdateEvent e) {
            path = "test/queue/enqueueTaskStatusUpdateEvent/" + e.getTaskId();
        } else {
            throw new RuntimeException("Unknown event type " + event.getClass() + ". If you need the ability to" +
                    " handle more types, please add the REST endpoints.");
        }
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/" + path))
                .header("Content-Type", APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(event)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Queueing event failed!" + response.body());
        }
    }

    private CompletableFuture<Void> awaitStreamingSubscription() {
        int cnt = getStreamingSubscribedCount();
        AtomicInteger initialCount = new AtomicInteger(cnt);

        return CompletableFuture.runAsync(() -> {
            try {
                boolean done = false;
                long end = System.currentTimeMillis() + 15000;
                while (System.currentTimeMillis() < end) {
                    int count = getStreamingSubscribedCount();
                    if (count > initialCount.get()) {
                        done = true;
                        break;
                    }
                    Thread.sleep(500);
                }
                if (!done) {
                    throw new RuntimeException("Timed out waiting for subscription");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted");
            }
        });
    }

    private int getStreamingSubscribedCount() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/streamingSubscribedCount"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body().trim();
            return Integer.parseInt(body);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void deletePushNotificationConfigInStore(String taskId, String configId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(("http://localhost:" + serverPort + "/test/task/" + taskId + "/config/" + configId)))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Deleting task failed!" + response.body());
        }
    }

    protected void savePushNotificationConfigInStore(String taskId, PushNotificationConfig notificationConfig) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task/" + taskId))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(notificationConfig)))
                .header("Content-Type", APPLICATION_JSON)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Creating task push notification config failed! " + response.body());
        }
    }

    /**
     * Get a client instance.
     */
    protected Client getClient() throws A2AClientException {
        if (client == null) {
            client = createClient(true);
        }
        return client;
    }

    /**
     * Get a client configured for non-streaming operations.
     */
    protected Client getNonStreamingClient() throws A2AClientException {
        if (nonStreamingClient == null) {
            nonStreamingClient = createClient(false);
        }
        return nonStreamingClient;
    }

    /**
     * Create a client with the specified streaming configuration.
     */
    private Client createClient(boolean streaming) throws A2AClientException {
        AgentCard agentCard = createTestAgentCard();
        ClientConfig clientConfig = createClientConfig(streaming);

        ClientBuilder clientBuilder = Client
                .builder(agentCard)
                .clientConfig(clientConfig);

        configureTransport(clientBuilder);

        return clientBuilder.build();
    }

    /**
     * Create a test agent card with the appropriate transport configuration.
     */
    private AgentCard createTestAgentCard() {
        return new AgentCard.Builder()
                .name("test-card")
                .description("A test agent card")
                .url(getTransportUrl())
                .version("1.0")
                .documentationUrl("http://example.com/docs")
                .preferredTransport(getTransportProtocol())
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .additionalInterfaces(List.of(new AgentInterface(getTransportProtocol(), getTransportUrl())))
                .protocolVersion("0.2.5")
                .build();
    }

    /**
     * Create client configuration with transport-specific settings.
     */
    private ClientConfig createClientConfig(boolean streaming) {
        return new ClientConfig.Builder()
                .setStreaming(streaming)
                .build();
    }

}