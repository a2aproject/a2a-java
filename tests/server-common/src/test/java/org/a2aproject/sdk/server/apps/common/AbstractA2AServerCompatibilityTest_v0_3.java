package org.a2aproject.sdk.server.apps.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.Event;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** v1 client compatibility scenarios for standalone A2A v0.3 servers. */
public abstract class AbstractA2AServerCompatibilityTest_v0_3 {

    protected static final Task MINIMAL_TASK = Task.builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new org.a2aproject.sdk.spec.TaskStatus(
                    org.a2aproject.sdk.spec.TaskState.TASK_STATE_SUBMITTED))
            .build();

    protected static final Message MESSAGE = Message.builder()
            .messageId("111")
            .role(Message.Role.ROLE_AGENT)
            .parts(new TextPart("test message"))
            .build();

    protected static final String APPLICATION_JSON = "application/json";

    protected final int serverPort;
    private final List<Client> createdClients = new ArrayList<>();
    private Client client;
    private Client nonStreamingClient;
    private Client pollingClient;

    protected AbstractA2AServerCompatibilityTest_v0_3(int serverPort) {
        this.serverPort = serverPort;
    }

    protected abstract String getTransportProtocol();

    protected abstract String getTransportUrl();

    protected abstract void configureTransport(ClientBuilder builder);

    protected AgentCard getAgentCard() {
        return A2A.getAgentCard(getTransportUrl(), Set.of("0.3"));
    }

    protected Client getClient() throws A2AClientException {
        if (client == null) {
            client = createClient(true);
        }
        return client;
    }

    protected Client getNonStreamingClient() throws A2AClientException {
        if (nonStreamingClient == null) {
            nonStreamingClient = createClient(false);
        }
        return nonStreamingClient;
    }

    protected Client getPollingClient() throws A2AClientException {
        if (pollingClient == null) {
            pollingClient = createPollingClient();
        }
        return pollingClient;
    }

    protected Client createClient(boolean streaming) throws A2AClientException {
        ClientBuilder builder = Client.builder(getAgentCard())
                .clientConfig(new ClientConfig.Builder().setStreaming(streaming).build());
        configureTransport(builder);
        Client created = builder.build();
        createdClients.add(created);
        return created;
    }

    protected Client createPollingClient() throws A2AClientException {
        ClientBuilder builder = Client.builder(getAgentCard())
                .clientConfig(new ClientConfig.Builder().setStreaming(false).setPolling(true).build());
        configureTransport(builder);
        Client created = builder.build();
        createdClients.add(created);
        return created;
    }

    private static final Task CANCEL_TASK = Task.builder(MINIMAL_TASK).id("cancel-task-123").build();
    private static final Task CANCEL_TASK_NOT_SUPPORTED =
            Task.builder(MINIMAL_TASK).id("cancel-task-not-supported-123").build();
    private static final Task SEND_MESSAGE_NOT_SUPPORTED =
            Task.builder(MINIMAL_TASK).id("task-not-supported-123").build();

    @Test
    public void testGetTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            Task response = getClient().getTask(new TaskQueryParams(MINIMAL_TASK.id()));
            assertEquals(MINIMAL_TASK.id(), response.id());
            assertEquals(MINIMAL_TASK.contextId(), response.contextId());
            assertEquals(TaskState.TASK_STATE_SUBMITTED, response.status().state());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testGetTaskNotFound() throws Exception {
        assertNull(getTaskFromTaskStore("non-existent-task"));
        A2AClientException error = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().getTask(new TaskQueryParams("non-existent-task")));
        assertInstanceOf(TaskNotFoundError.class, error.getCause());
    }

    @Test
    public void testCancelTaskSuccess() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK);
        try {
            Task task = getClient().cancelTask(new CancelTaskParams(CANCEL_TASK.id()));
            assertEquals(CANCEL_TASK.id(), task.id());
            assertEquals(TaskState.TASK_STATE_CANCELED, task.status().state());
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK.id());
        }
    }

    @Test
    public void testCancelTaskNotSupported() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED);
        try {
            A2AClientException error = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                    () -> getClient().cancelTask(new CancelTaskParams(CANCEL_TASK_NOT_SUPPORTED.id())));
            assertInstanceOf(UnsupportedOperationError.class, error.getCause());
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED.id());
        }
    }

    @Test
    public void testCancelTaskNotFound() throws Exception {
        A2AClientException error = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().cancelTask(new CancelTaskParams("non-existent-task")));
        assertInstanceOf(TaskNotFoundError.class, error.getCause());
    }

    @Test
    public void testSendMessageNewMessageSuccess() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Message> received = new AtomicReference<>();
        AtomicBoolean unexpected = new AtomicBoolean();
        getNonStreamingClient().sendMessage(MESSAGE, List.of((event, card) -> {
            if (event instanceof MessageEvent messageEvent && latch.getCount() > 0) {
                received.set(messageEvent.getMessage());
                latch.countDown();
            } else {
                unexpected.set(true);
            }
        }), null);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertFalse(unexpected.get());
        assertEquals(MESSAGE.messageId(), received.get().messageId());
        assertEquals(MESSAGE.role(), received.get().role());
        Part<?> part = received.get().parts().get(0);
        assertInstanceOf(TextPart.class, part);
        assertEquals("test message", ((TextPart) part).text());
    }

    @Test
    public void testSendMessageWithInlinePushConfiguration() throws Exception {
        sendMessageWithInlinePushConfiguration(false);
    }

    @Test
    public void testSendStreamingMessageWithInlinePushConfiguration() throws Exception {
        sendMessageWithInlinePushConfiguration(true);
    }

    private void sendMessageWithInlinePushConfiguration(boolean streaming) throws Exception {
        Client sendingClient = streaming ? getClient() : getNonStreamingClient();
        for (String taskId : new String[] {null, ""}) {
            TaskPushNotificationConfig push = new TaskPushNotificationConfig(
                    "inline-config", taskId, "http://localhost:" + serverPort + "/callback", null, null, null);
            Message taskMessage = Message.builder(MESSAGE).parts(new TextPart("#a2a-delegated#inline push")).build();
            MessageSendParams request = new MessageSendParams(taskMessage,
                    MessageSendConfiguration.builder().taskPushNotificationConfig(push).build(), null);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Task> received = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();
            try {
                sendingClient.sendMessage(request, List.of((event, card) -> {
                    Task task = event instanceof TaskEvent initial ? initial.getTask()
                            : event instanceof TaskUpdateEvent update ? update.getTask() : null;
                    if (task != null) {
                        received.set(task);
                        if (task.status().state() == TaskState.TASK_STATE_COMPLETED) {
                            latch.countDown();
                        }
                    }
                }), throwable -> {
                    if (throwable != null && !isStreamClosedError(throwable)) {
                        error.set(throwable);
                        latch.countDown();
                    }
                }, null);

                assertTrue(latch.await(10, TimeUnit.SECONDS));
                assertNull(error.get());
                assertNotNull(received.get());
                assertEquals(TaskState.TASK_STATE_COMPLETED, received.get().status().state());
                TaskPushNotificationConfig stored = getClient().getTaskPushNotificationConfiguration(
                        new GetTaskPushNotificationConfigParams(received.get().id(), "inline-config"));
                assertEquals("inline-config", stored.id());
                assertEquals(received.get().id(), stored.taskId());
                assertEquals(push.url(), stored.url());
            } finally {
                if (received.get() != null) {
                    deletePushNotificationConfigInStore(received.get().id(), "inline-config");
                    deleteTaskInTaskStore(received.get().id());
                }
            }
        }
    }

    @Test
    public void testRequestScopedBeanAvailableOnAgentExecutorThread() throws Exception {
        Message message = Message.builder().messageId("request-scoped-test").role(Message.Role.ROLE_USER)
                .parts(new TextPart("request-scoped:test")).build();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Task> received = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        getNonStreamingClient().sendMessage(message, List.of((event, card) -> {
            if (event instanceof TaskEvent taskEvent) {
                received.set(taskEvent.getTask());
                latch.countDown();
            } else if (event instanceof TaskUpdateEvent updateEvent) {
                received.set(updateEvent.getTask());
                if (updateEvent.getTask().status().state() == TaskState.TASK_STATE_COMPLETED) {
                    latch.countDown();
                }
            }
        }), error::set);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(TaskState.TASK_STATE_COMPLETED, received.get().status().state());
        assertInstanceOf(TextPart.class, received.get().artifacts().get(0).parts().get(0));
        assertEquals("request-scoped:request-scoped-value",
                ((TextPart) received.get().artifacts().get(0).parts().get(0)).text());
    }

    @Test
    public void testSendMessageExistingTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            Message message = Message.builder(MESSAGE).taskId(MINIMAL_TASK.id())
                    .contextId(MINIMAL_TASK.contextId()).build();
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Message> received = new AtomicReference<>();
            getNonStreamingClient().sendMessage(message, List.of((event, card) -> {
                if (event instanceof MessageEvent messageEvent) {
                    received.set(messageEvent.getMessage());
                    latch.countDown();
                }
            }), null);
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertEquals(MESSAGE.messageId(), received.get().messageId());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testSetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                    .id("c295ea44-7543-4f78-b524-7a38915ad6e4").taskId(MINIMAL_TASK.id())
                    .url("http://example.com").tenant("").build();
            TaskPushNotificationConfig result = getClient().createTaskPushNotificationConfiguration(config);
            assertEquals(config.id(), result.id());
            assertEquals(config.url(), result.url());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "c295ea44-7543-4f78-b524-7a38915ad6e4");
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testGetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                    .id("c295ea44-7543-4f78-b524-7a38915ad6e4").taskId(MINIMAL_TASK.id())
                    .url("http://example.com").tenant("").build();
            getClient().createTaskPushNotificationConfiguration(config);
            TaskPushNotificationConfig result = getClient().getTaskPushNotificationConfiguration(
                    new GetTaskPushNotificationConfigParams(MINIMAL_TASK.id(), config.id()));
            assertEquals(config.url(), result.url());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "c295ea44-7543-4f78-b524-7a38915ad6e4");
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testDefaultPushConfigurationLookupWithSeveralConfigurations() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig("other-1", "http://example.com/one"));
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig("other-2", "http://example.com/two"));
            // REST/gRPC turn the default into an explicit ID on the server, permitting legacy fallback.
            // JSON-RPC preserves omission and must not take that explicit-ID fallback.
            if ("JSONRPC".equals(getTransportProtocol())) {
                A2AClientException missing = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                        () -> getClient().getTaskPushNotificationConfiguration(
                                new GetTaskPushNotificationConfigParams(MINIMAL_TASK.id())));
                assertInstanceOf(TaskNotFoundError.class, missing.getCause());
            }
            savePushNotificationConfigInStore(MINIMAL_TASK.id(),
                    pushConfig(MINIMAL_TASK.id(), "http://example.com/default"));
            TaskPushNotificationConfig result = getClient().getTaskPushNotificationConfiguration(
                    new GetTaskPushNotificationConfigParams(MINIMAL_TASK.id()));
            assertEquals(MINIMAL_TASK.id(), result.id());
            assertEquals("http://example.com/default", result.url());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "other-1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "other-2");
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), MINIMAL_TASK.id());
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testError() throws Exception {
        saveTaskInTaskStore(SEND_MESSAGE_NOT_SUPPORTED);
        try {
            Message message = Message.builder(MESSAGE).taskId(SEND_MESSAGE_NOT_SUPPORTED.id())
                    .contextId(SEND_MESSAGE_NOT_SUPPORTED.contextId()).build();
            A2AClientException error = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                    () -> getNonStreamingClient().sendMessage(message));
            assertInstanceOf(UnsupportedOperationError.class, error.getCause());
        } finally {
            deleteTaskInTaskStore(SEND_MESSAGE_NOT_SUPPORTED.id());
        }
    }

    @Test
    public void testSendMessageStreamNewMessageSuccess() throws Exception {
        sendStreamingMessage(false);
    }

    @Test
    public void testSendMessageStreamExistingTaskSuccess() throws Exception {
        sendStreamingMessage(true);
    }

    private void sendStreamingMessage(boolean existingTask) throws Exception {
        if (existingTask) {
            saveTaskInTaskStore(MINIMAL_TASK);
        }
        try {
            Message.Builder messageBuilder = Message.builder(MESSAGE);
            if (existingTask) {
                messageBuilder.taskId(MINIMAL_TASK.id()).contextId(MINIMAL_TASK.contextId());
            }
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Message> received = new AtomicReference<>();
            AtomicBoolean unexpected = new AtomicBoolean();
            AtomicReference<Throwable> error = new AtomicReference<>();
            getClient().sendMessage(messageBuilder.build(), List.of((event, card) -> {
                if (event instanceof MessageEvent messageEvent && latch.getCount() > 0) {
                    received.set(messageEvent.getMessage());
                    latch.countDown();
                } else {
                    unexpected.set(true);
                }
            }), throwable -> {
                if (!isStreamClosedError(throwable)) {
                    error.set(throwable);
                }
                latch.countDown();
            });
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            assertFalse(unexpected.get());
            assertNull(error.get());
            assertEquals(MESSAGE.messageId(), received.get().messageId());
            assertEquals(MESSAGE.role(), received.get().role());
            assertEquals("test message", ((TextPart) received.get().parts().get(0)).text());
        } finally {
            if (existingTask) {
                deleteTaskInTaskStore(MINIMAL_TASK.id());
            }
        }
    }

    protected boolean isStreamClosedError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.io.EOFException
                    || current instanceof java.util.concurrent.CancellationException
                    || (current instanceof IOException && current.getMessage() != null
                        && current.getMessage().contains("cancelled"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Test
    public void testResubscribeExistingTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            ensureQueueForTask(MINIMAL_TASK.id());
            CountDownLatch events = new CountDownLatch(2);
            AtomicReference<TaskArtifactUpdateEvent> artifact = new AtomicReference<>();
            AtomicReference<TaskStatusUpdateEvent> status = new AtomicReference<>();
            AtomicBoolean initialTask = new AtomicBoolean();
            AtomicReference<Throwable> error = new AtomicReference<>();
            CompletableFuture<Void> subscription = awaitStreamingSubscription();
            getClient().subscribeToTask(new TaskIdParams(MINIMAL_TASK.id()), List.of((event, card) -> {
                if (!initialTask.getAndSet(true)) {
                    assertInstanceOf(TaskEvent.class, event);
                    return;
                }
                if (event instanceof TaskUpdateEvent update) {
                    if (update.getUpdateEvent() instanceof TaskArtifactUpdateEvent value) {
                        artifact.set(value);
                        events.countDown();
                    } else if (update.getUpdateEvent() instanceof TaskStatusUpdateEvent value) {
                        status.set(value);
                        events.countDown();
                    }
                }
            }), failure -> { if (!isStreamClosedError(failure)) error.set(failure); });
            subscription.get(15, TimeUnit.SECONDS);
            enqueueEventOnServer(TaskArtifactUpdateEvent.builder()
                    .taskId(MINIMAL_TASK.id())
                    .contextId(MINIMAL_TASK.contextId())
                    .artifact(Artifact.builder()
                            .artifactId("11")
                            .parts(new TextPart("text"))
                            .build())
                    .build());
            enqueueEventOnServer(TaskStatusUpdateEvent.builder()
                    .taskId(MINIMAL_TASK.id())
                    .contextId(MINIMAL_TASK.contextId())
                    .status(new org.a2aproject.sdk.spec.TaskStatus(TaskState.TASK_STATE_COMPLETED))
                    .build());
            assertTrue(events.await(30, TimeUnit.SECONDS));
            assertNull(error.get());
            assertEquals(MINIMAL_TASK.id(), artifact.get().taskId());
            assertEquals(TaskState.TASK_STATE_COMPLETED, status.get().status().state());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testResubscribeNoExistingTaskError() throws Exception {
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        getClient().subscribeToTask(new TaskIdParams("non-existent-task"), List.of(), failure -> {
            error.set(failure);
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(error.get());
    }

    @Test
    public void testMainQueueReferenceCountingWithMultipleConsumers() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            ensureQueueForTask(MINIMAL_TASK.id());
            CountDownLatch firstEvent = new CountDownLatch(1);
            CountDownLatch secondEvent = new CountDownLatch(1);
            BiConsumer<org.a2aproject.sdk.client.ClientEvent, AgentCard> firstConsumer = (event, card) -> {
                if (event instanceof TaskUpdateEvent update
                        && update.getUpdateEvent() instanceof TaskArtifactUpdateEvent) {
                    firstEvent.countDown();
                }
            };
            BiConsumer<org.a2aproject.sdk.client.ClientEvent, AgentCard> secondConsumer = (event, card) -> {
                if (event instanceof TaskUpdateEvent update
                        && update.getUpdateEvent() instanceof TaskArtifactUpdateEvent) {
                    secondEvent.countDown();
                }
            };
            CompletableFuture<Void> firstSubscription = awaitStreamingSubscription();
            getClient().subscribeToTask(new TaskIdParams(MINIMAL_TASK.id()), List.of(firstConsumer), null);
            firstSubscription.get(15, TimeUnit.SECONDS);
            enqueueEventOnServer(TaskArtifactUpdateEvent.builder().taskId(MINIMAL_TASK.id())
                    .contextId(MINIMAL_TASK.contextId()).artifact(Artifact.builder().artifactId("artifact-1")
                            .parts(new TextPart("First artifact")).build()).build());
            assertTrue(firstEvent.await(15, TimeUnit.SECONDS));
            assertTrue(getChildQueueCount(MINIMAL_TASK.id()) >= 2);

            CompletableFuture<Void> secondSubscription = awaitStreamingSubscription();
            getClient().subscribeToTask(new TaskIdParams(MINIMAL_TASK.id()), List.of(secondConsumer), null);
            secondSubscription.get(15, TimeUnit.SECONDS);
            enqueueEventOnServer(TaskArtifactUpdateEvent.builder().taskId(MINIMAL_TASK.id())
                    .contextId(MINIMAL_TASK.contextId()).artifact(Artifact.builder().artifactId("artifact-2")
                            .parts(new TextPart("Second artifact")).build()).build());
            assertTrue(secondEvent.await(15, TimeUnit.SECONDS));
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testNonBlockingWithMultipleMessages() throws Exception {
        Client polling = getPollingClient();
        List<CompletableFuture<Void>> calls = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                Task task = Task.builder(MINIMAL_TASK).id("compat-held-task-" + i).build();
                tasks.add(task);
                saveTaskInTaskStore(task);
                Message message = Message.builder(MESSAGE).messageId("non-blocking-" + i)
                        .taskId(task.id()).contextId(task.contextId()).parts(new TextPart("compat-hold:")).build();
                calls.add(CompletableFuture.runAsync(() -> polling.sendMessage(message,
                        List.of((event, card) -> assertInstanceOf(TaskEvent.class, event)), null)));
            }
            for (int i = 0; i < calls.size(); i++) {
                // The executor is still held; a blocking request cannot complete within this deadline.
                calls.get(i).get(10, TimeUnit.SECONDS);
                Task working = awaitTaskState(tasks.get(i).id(), TaskState.TASK_STATE_WORKING);
                assertEquals(TaskState.TASK_STATE_WORKING, working.status().state());
            }
        } finally {
            cleanupHeldTasks(tasks, calls);
        }
    }

    private void cleanupHeldTasks(List<Task> tasks, List<CompletableFuture<Void>> calls) throws Exception {
        List<Throwable> failures = new ArrayList<>();
        try {
            // Independent release markers cannot be overwritten by an executor's task update.
            // Release all executors before waiting for any individual request to finish.
            for (Task task : tasks) {
                try {
                    saveTaskInTaskStore(Task.builder(task).id(task.id() + "-release").build());
                } catch (Exception e) {
                    failures.add(e);
                }
            }
            for (int i = 0; i < calls.size(); i++) {
                try {
                    calls.get(i).get(10, TimeUnit.SECONDS);
                    awaitTaskState(tasks.get(i).id(), TaskState.TASK_STATE_COMPLETED);
                } catch (Exception | AssertionError e) {
                    failures.add(e);
                }
            }
        } finally {
            for (Task task : tasks) {
                for (String id : List.of(task.id(), task.id() + "-release")) {
                    try {
                        deleteTaskInTaskStore(id);
                    } catch (Exception e) {
                        failures.add(e);
                    }
                }
            }
        }
        if (!failures.isEmpty()) {
            Exception failure = new Exception("Could not clean up held compatibility tasks");
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
    }

    private Task awaitTaskState(String taskId, TaskState expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        Task task;
        do {
            task = getTaskFromTaskStore(taskId);
            if (task != null && task.status().state() == expected) {
                return task;
            }
            Thread.sleep(25);
        } while (System.nanoTime() < deadline);
        fail("Task " + taskId + " did not reach " + expected);
        throw new AssertionError("unreachable");
    }

    @Test
    public void testMainQueueStaysOpenForNonFinalTasks() throws Exception {
        String taskId = "fire-and-forget-task-integration";
        Task task = Task.builder(MINIMAL_TASK).id(taskId).status(new org.a2aproject.sdk.spec.TaskStatus(
                TaskState.TASK_STATE_WORKING)).build();
        saveTaskInTaskStore(task);
        try {
            ensureQueueWithoutConsumer(taskId);
            assertEquals(0, getChildQueueCount(taskId), "Non-final task queue must stay open with no consumers");
            CountDownLatch updateReceived = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            CompletableFuture<Void> subscription = awaitStreamingSubscription();
            getClient().subscribeToTask(new TaskIdParams(taskId), List.of((event, card) -> {
                if (event instanceof TaskUpdateEvent update
                        && update.getUpdateEvent() instanceof TaskStatusUpdateEvent status
                        && taskId.equals(status.taskId())
                        && status.status().state() == TaskState.TASK_STATE_WORKING) {
                    updateReceived.countDown();
                }
            }), failure -> {
                if (failure != null && !isStreamClosedError(failure)) {
                    error.set(failure);
                    updateReceived.countDown();
                }
            });
            subscription.get(15, TimeUnit.SECONDS);
            assertEquals(1, getChildQueueCount(taskId), "Late subscription must create an active consumer");
            enqueueEventOnServer(TaskStatusUpdateEvent.builder().taskId(taskId).contextId(task.contextId())
                    .status(new org.a2aproject.sdk.spec.TaskStatus(TaskState.TASK_STATE_WORKING)).build());
            assertTrue(updateReceived.await(30, TimeUnit.SECONDS), "Must receive the enqueued status update");
            assertNull(error.get(), "Subscription must not fail");
            assertEquals(1, getChildQueueCount(taskId), "Non-final update must leave the consumer and main queue open");
        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

    @Test
    public void testMainQueueClosesForFinalizedTasks() throws Exception {
        String taskId = "finalized-task-integration";
        saveTaskInTaskStore(Task.builder(MINIMAL_TASK).id(taskId).build());
        try {
            ensureQueueWithoutConsumer(taskId);
            assertEquals(0, getChildQueueCount(taskId));
            CountDownLatch finalUpdateReceived = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            CompletableFuture<Void> subscription = awaitStreamingSubscription();
            getClient().subscribeToTask(new TaskIdParams(taskId), List.of((event, card) -> {
                if (event instanceof TaskUpdateEvent update
                        && update.getUpdateEvent() instanceof TaskStatusUpdateEvent status
                        && taskId.equals(status.taskId())
                        && status.status().state() == TaskState.TASK_STATE_COMPLETED
                        && status.isFinal()) {
                    finalUpdateReceived.countDown();
                }
            }), failure -> {
                if (failure != null && !isStreamClosedError(failure)) {
                    error.set(failure);
                    finalUpdateReceived.countDown();
                }
            });
            subscription.get(15, TimeUnit.SECONDS);
            assertEquals(1, getChildQueueCount(taskId), "Subscription must be active before finalization");
            enqueueEventOnServer(TaskStatusUpdateEvent.builder().taskId(taskId).contextId(MINIMAL_TASK.contextId())
                    .status(new org.a2aproject.sdk.spec.TaskStatus(TaskState.TASK_STATE_COMPLETED)).build());
            assertTrue(finalUpdateReceived.await(30, TimeUnit.SECONDS), "Must receive the final status update");
            assertNull(error.get(), "Subscription must not fail before delivering the final update");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (getChildQueueCount(taskId) != -1 && System.nanoTime() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(-1, getChildQueueCount(taskId), "Finalized task must close or remove its main queue");
            assertNull(error.get(), "Subscription must close without an unexpected error");
        } finally {
            deleteTaskInTaskStore(taskId);
        }
    }

    private void ensureQueueWithoutConsumer(String taskId) throws Exception {
        sendTestRequest("/test/queue/ensure/" + taskId + "?withoutConsumer=true", "POST", "", 200);
    }

    private CompletableFuture<Void> awaitStreamingSubscription() {
        int initial = getStreamingSubscribedCount();
        return CompletableFuture.runAsync(() -> {
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline) {
                if (getStreamingSubscribedCount() > initial) {
                    return;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            throw new IllegalStateException("Timed out waiting for streaming subscription");
        });
    }

    @Test
    public void testListPushNotificationConfigsWithConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig("config1", "http://example.com"));
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig("config2", "http://example.com"));
            ListTaskPushNotificationConfigsResult result = getClient()
                    .listTaskPushNotificationConfigurations(new ListTaskPushNotificationConfigsParams(MINIMAL_TASK.id()));
            assertEquals(2, result.size());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testListPushNotificationConfigsWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig(MINIMAL_TASK.id(), "http://example.com"));
            assertEquals(1, getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigsParams(MINIMAL_TASK.id())).size());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), MINIMAL_TASK.id());
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testListPushNotificationConfigsTaskNotFound() throws Exception {
        A2AClientException error = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().listTaskPushNotificationConfigurations(
                        new ListTaskPushNotificationConfigsParams("non-existent-task")));
        assertInstanceOf(TaskNotFoundError.class, error.getCause());
    }

    @Test
    public void testListPushNotificationConfigsEmptyList() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            assertEquals(0, getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigsParams(MINIMAL_TASK.id())).size());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithValidConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig("config1", "http://example.com"));
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig("config2", "http://example.com"));
            getClient().deleteTaskPushNotificationConfigurations(
                    new DeleteTaskPushNotificationConfigParams(MINIMAL_TASK.id(), "config1"));
            assertEquals(1, getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigsParams(MINIMAL_TASK.id())).size());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithNonExistingConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig("config1", "http://example.com"));
            getClient().deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams(
                    MINIMAL_TASK.id(), "non-existent-config-id"));
            assertEquals(1, getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigsParams(MINIMAL_TASK.id())).size());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), "config1");
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    @Test
    public void testDeletePushNotificationConfigTaskNotFound() throws Exception {
        A2AClientException error = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().deleteTaskPushNotificationConfigurations(
                        new DeleteTaskPushNotificationConfigParams("non-existent-task", "config")));
        assertInstanceOf(TaskNotFoundError.class, error.getCause());
    }

    @Test
    public void testDeletePushNotificationConfigSetWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            savePushNotificationConfigInStore(MINIMAL_TASK.id(), pushConfig(MINIMAL_TASK.id(), "http://example.com"));
            getClient().deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams(
                    MINIMAL_TASK.id(), MINIMAL_TASK.id()));
            assertEquals(0, getClient().listTaskPushNotificationConfigurations(
                    new ListTaskPushNotificationConfigsParams(MINIMAL_TASK.id())).size());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.id(), MINIMAL_TASK.id());
            deleteTaskInTaskStore(MINIMAL_TASK.id());
        }
    }

    private static TaskPushNotificationConfig pushConfig(String id, String url) {
        return TaskPushNotificationConfig.builder().id(id).url(url).build();
    }

    @Test
    public void testNonObjectDataPartsAreRejectedForBlockingAndStreamingSends() throws Exception {
        for (Client sendingClient : List.of(getNonStreamingClient(), getClient())) {
            for (Object data : List.of("value", 42, true, List.of("item"))) {
                Message message = Message.builder().messageId("unsupported-data").role(Message.Role.ROLE_USER)
                        .parts(new DataPart(data)).build();

                A2AClientException exception = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                        () -> sendingClient.sendMessage(message));

                assertInstanceOf(UnsupportedOperationError.class, exception.getCause());
            }
        }
    }

    @Test
    public void testUnsupportedOperationsAreRejectedLocally() throws Exception {
        A2AClientException listError = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().listTasks(new ListTasksParams()));
        assertInstanceOf(UnsupportedOperationError.class, listError.getCause());

        A2AClientException cardError = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().getExtendedAgentCard());
        assertInstanceOf(UnsupportedOperationError.class, cardError.getCause());

        A2AClientException tenantError = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().getTask(new TaskQueryParams("task-123", null, "tenant-a")));
        assertInstanceOf(UnsupportedOperationError.class, tenantError.getCause());

        A2AClientException paginationError = org.junit.jupiter.api.Assertions.assertThrows(A2AClientException.class,
                () -> getClient().listTaskPushNotificationConfigurations(
                        new ListTaskPushNotificationConfigsParams("task-123", 10, "next", null)));
        assertInstanceOf(UnsupportedOperationError.class, paginationError.getCause());
    }

    protected final void registerCreatedClient(Client client) {
        createdClients.add(client);
    }

    @AfterEach
    void closeCreatedClients() {
        createdClients.forEach(Client::close);
        createdClients.clear();
        client = null;
        nonStreamingClient = null;
        pollingClient = null;
    }

    protected void saveTaskInTaskStore(Task task) throws Exception {
        sendTestRequest("/test/task", "POST", JsonUtil.toJson(task), 200);
    }

    protected Task getTaskFromTaskStore(String taskId) throws Exception {
        HttpResponse<String> response = testRequest("/test/task/" + taskId, "GET", null);
        if (response.statusCode() == 404) {
            return null;
        }
        assertEquals(200, response.statusCode(), response.body());
        return JsonUtil.fromJson(response.body(), Task.class);
    }

    protected void deleteTaskInTaskStore(String taskId) throws Exception {
        sendTestRequest("/test/task/" + taskId, "DELETE", null, 200);
    }

    protected void ensureQueueForTask(String taskId) throws Exception {
        sendTestRequest("/test/queue/ensure/" + taskId, "POST", "", 200);
    }

    protected void enqueueEventOnServer(Event event) throws Exception {
        String path;
        if (event instanceof TaskArtifactUpdateEvent artifact) {
            path = "/test/queue/enqueueTaskArtifactUpdateEvent/" + artifact.taskId();
        } else if (event instanceof TaskStatusUpdateEvent status) {
            path = "/test/queue/enqueueTaskStatusUpdateEvent/" + status.taskId();
        } else {
            throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
        }
        sendTestRequest(path, "POST", JsonUtil.toJson(event), 200);
    }

    protected int getChildQueueCount(String taskId) {
        try {
            return Integer.parseInt(testRequest("/test/queue/childCount/" + taskId, "GET", null)
                    .body().trim());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected int getStreamingSubscribedCount() {
        try {
            return Integer.parseInt(testRequest("/test/streamingSubscribedCount", "GET", null).body().trim());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void deletePushNotificationConfigInStore(String taskId, String configId) throws Exception {
        sendTestRequest("/test/task/" + taskId + "/config/" + configId, "DELETE", null, 200);
    }

    protected void savePushNotificationConfigInStore(String taskId,
            TaskPushNotificationConfig notificationConfig) throws Exception {
        sendTestRequest("/test/task/" + taskId, "POST", JsonUtil.toJson(notificationConfig), 200);
    }

    private void sendTestRequest(String path, String method, String body, int expectedStatus) throws Exception {
        HttpResponse<String> response = testRequest(path, method, body);
        assertEquals(expectedStatus, response.statusCode(), response.body());
    }

    private HttpResponse<String> testRequest(String path, String method, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + path));
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .header("Content-Type", APPLICATION_JSON);
        }
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()
                .send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
