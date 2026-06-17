package org.a2aproject.sdk.server.apps.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for task authorization integration tests.
 * <p>
 * Verifies that {@link org.a2aproject.sdk.server.auth.TaskAuthorizationProvider}
 * is enforced end-to-end through the transport layer with two distinct users.
 */
public abstract class AbstractA2AServerWithTaskAuthorizationTest {

    protected static final String USER_A = "testuser";
    protected static final String USER_A_PASSWORD = "testpass";
    protected static final String USER_B = "userB";
    protected static final String USER_B_PASSWORD = "passB";
    protected static final String BASIC_AUTH_SCHEME_NAME = "basicAuth";

    protected abstract String getTransportProtocol();

    protected abstract String getTransportUrl();

    protected abstract void configureTransportWithCredentials(ClientBuilder builder, String username, String password);

    protected Client createClient(String username, String password) throws A2AClientException {
        AgentCard agentCard = fetchAgentCardFromServer();
        ClientConfig clientConfig = new ClientConfig.Builder().setStreaming(false).build();
        ClientBuilder clientBuilder = Client.builder(agentCard).clientConfig(clientConfig);
        configureTransportWithCredentials(clientBuilder, username, password);
        return clientBuilder.build();
    }

    protected AgentCard fetchAgentCardFromServer() {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getTransportUrl() + "/.well-known/agent-card.json"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch agent card: " + response.statusCode());
            }
            return JsonUtil.fromJson(response.body(), AgentCard.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch AgentCard from server", e);
        }
    }

    protected static String getEncodedCredentials(String username, String password) {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    protected Task sendMessageAndGetTask(Client client, String messageText) throws Exception {
        Message message = Message.builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message.Role.ROLE_USER)
                .parts(new TextPart("a2a-local:" + messageText))
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Task> receivedTask = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        client.sendMessage(message, List.of((event, agentCard) -> {
            if (event instanceof TaskEvent te) {
                receivedTask.set(te.getTask());
                if (te.getTask().status().state() == TaskState.TASK_STATE_COMPLETED) {
                    latch.countDown();
                }
            } else if (event instanceof TaskUpdateEvent tue) {
                receivedTask.set(tue.getTask());
                if (tue.getTask().status().state() == TaskState.TASK_STATE_COMPLETED) {
                    latch.countDown();
                }
            }
        }), error -> {
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Task should complete within timeout");
        assertNull(errorRef.get(), "Should not have received an error: " + errorRef.get());

        Task task = receivedTask.get();
        assertNotNull(task, "Should have received a task");
        assertEquals(TaskState.TASK_STATE_COMPLETED, task.status().state());
        return task;
    }

    @Test
    public void testOwnerCanGetOwnTask() throws Exception {
        Client clientA = createClient(USER_A, USER_A_PASSWORD);
        Task task = sendMessageAndGetTask(clientA, "owner-get-test");

        Task retrieved = clientA.getTask(new TaskQueryParams(task.id()));
        assertNotNull(retrieved);
        assertEquals(task.id(), retrieved.id());
    }

    @Test
    public void testNonOwnerCannotGetTask() throws Exception {
        Client clientA = createClient(USER_A, USER_A_PASSWORD);
        Task task = sendMessageAndGetTask(clientA, "non-owner-get-test");

        Client clientB = createClient(USER_B, USER_B_PASSWORD);
        A2AClientException error = assertThrows(A2AClientException.class, () ->
                clientB.getTask(new TaskQueryParams(task.id())));
        assertInstanceOf(TaskNotFoundError.class, error.getCause());
    }

    @Test
    public void testOwnerCanCancelOwnTask() throws Exception {
        Client clientA = createClient(USER_A, USER_A_PASSWORD);
        Task task = sendMessageAndGetTask(clientA, "owner-cancel-test");

        try {
            clientA.cancelTask(new CancelTaskParams(task.id()));
        } catch (A2AClientException e) {
            // UnsupportedOperationError is acceptable (task already completed)
            // but TaskNotFoundError means auth failed
            assertFalse(e.getCause() instanceof TaskNotFoundError,
                    "Owner should not get TaskNotFoundError when canceling own task");
        }
    }

    @Test
    public void testNonOwnerCannotCancelTask() throws Exception {
        Client clientA = createClient(USER_A, USER_A_PASSWORD);
        Task task = sendMessageAndGetTask(clientA, "non-owner-cancel-test");

        Client clientB = createClient(USER_B, USER_B_PASSWORD);
        A2AClientException error = assertThrows(A2AClientException.class, () ->
                clientB.cancelTask(new CancelTaskParams(task.id())));
        assertInstanceOf(TaskNotFoundError.class, error.getCause());
    }

    @Test
    public void testListTasksShowsOnlyOwnTasks() throws Exception {
        Client clientA = createClient(USER_A, USER_A_PASSWORD);
        Task taskA1 = sendMessageAndGetTask(clientA, "list-test-a1");
        Task taskA2 = sendMessageAndGetTask(clientA, "list-test-a2");

        Client clientB = createClient(USER_B, USER_B_PASSWORD);
        Task taskB1 = sendMessageAndGetTask(clientB, "list-test-b1");

        ListTasksParams listParams = ListTasksParams.builder().tenant("").build();

        ListTasksResult resultA = clientA.listTasks(listParams);
        Set<String> taskIdsA = resultA.tasks().stream().map(Task::id).collect(Collectors.toSet());
        assertTrue(taskIdsA.contains(taskA1.id()), "UserA should see taskA1");
        assertTrue(taskIdsA.contains(taskA2.id()), "UserA should see taskA2");
        assertFalse(taskIdsA.contains(taskB1.id()), "UserA should NOT see taskB1");

        ListTasksResult resultB = clientB.listTasks(listParams);
        Set<String> taskIdsB = resultB.tasks().stream().map(Task::id).collect(Collectors.toSet());
        assertTrue(taskIdsB.contains(taskB1.id()), "UserB should see taskB1");
        assertFalse(taskIdsB.contains(taskA1.id()), "UserB should NOT see taskA1");
        assertFalse(taskIdsB.contains(taskA2.id()), "UserB should NOT see taskA2");
    }

    @Test
    public void testUnauthorizedLooksLikeNotFound() throws Exception {
        Client clientA = createClient(USER_A, USER_A_PASSWORD);
        Task task = sendMessageAndGetTask(clientA, "info-hiding-test");

        Client clientB = createClient(USER_B, USER_B_PASSWORD);

        A2AClientException unauthorizedError = assertThrows(A2AClientException.class, () ->
                clientB.getTask(new TaskQueryParams(task.id())));
        A2AClientException notFoundError = assertThrows(A2AClientException.class, () ->
                clientB.getTask(new TaskQueryParams(UUID.randomUUID().toString())));

        assertInstanceOf(TaskNotFoundError.class, unauthorizedError.getCause());
        assertInstanceOf(TaskNotFoundError.class, notFoundError.getCause());
    }

    @Test
    public void testBothUsersCanCreateTasks() throws Exception {
        Client clientA = createClient(USER_A, USER_A_PASSWORD);
        Task taskA = sendMessageAndGetTask(clientA, "create-test-a");
        assertNotNull(taskA.id());

        Client clientB = createClient(USER_B, USER_B_PASSWORD);
        Task taskB = sendMessageAndGetTask(clientB, "create-test-b");
        assertNotNull(taskB.id());
    }
}
