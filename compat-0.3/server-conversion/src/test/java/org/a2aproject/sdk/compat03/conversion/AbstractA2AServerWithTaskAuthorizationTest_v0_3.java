package org.a2aproject.sdk.compat03.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.compat03.client.Client_v0_3;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.TaskEvent_v0_3;
import org.a2aproject.sdk.compat03.client.TaskUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.client.config.ClientConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

/**
 * Abstract base class for v0.3 task authorization integration tests.
 * <p>
 * Mirrors {@link org.a2aproject.sdk.server.apps.common.AbstractA2AServerWithTaskAuthorizationTest}
 * but uses v0.3 client types. Tests verify that
 * {@link org.a2aproject.sdk.server.auth.TaskAuthorizationProvider} is enforced
 * through the v0.3 compatibility layer.
 * <p>
 * Note: v0.3 has no {@code listTasks()} API, so {@code testListTasksShowsOnlyOwnTasks}
 * is not included.
 */
public abstract class AbstractA2AServerWithTaskAuthorizationTest_v0_3 {

    protected static final String USER_A = "testuser";
    protected static final String USER_A_PASSWORD = "testpass";
    protected static final String USER_B = "userB";
    protected static final String USER_B_PASSWORD = "passB";
    protected static final String BASIC_AUTH_SCHEME_NAME = "basicAuth";

    protected final int serverPort;

    protected AbstractA2AServerWithTaskAuthorizationTest_v0_3(int serverPort) {
        this.serverPort = serverPort;
    }

    protected abstract String getTransportProtocol();

    protected abstract String getTransportUrl();

    protected abstract void configureTransportWithCredentials(ClientBuilder_v0_3 builder, String username, String password);

    protected Client_v0_3 createClient(String username, String password) throws A2AClientException_v0_3 {
        AgentCard_v0_3 agentCard = createTestAgentCard();
        ClientConfig_v0_3 clientConfig = new ClientConfig_v0_3.Builder().setStreaming(false).build();
        ClientBuilder_v0_3 clientBuilder = Client_v0_3.builder(agentCard).clientConfig(clientConfig);
        configureTransportWithCredentials(clientBuilder, username, password);
        return clientBuilder.build();
    }

    private AgentCard_v0_3 createTestAgentCard() {
        return new AgentCard_v0_3.Builder()
                .name("test-card")
                .description("A test agent card")
                .url(getTransportUrl())
                .version("1.0")
                .preferredTransport(getTransportProtocol())
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .additionalInterfaces(List.of(new AgentInterface_v0_3(getTransportProtocol(), getTransportUrl())))
                .securitySchemes(Map.of(
                        BASIC_AUTH_SCHEME_NAME,
                        new HTTPAuthSecurityScheme_v0_3.Builder()
                                .scheme("basic")
                                .description("HTTP Basic authentication")
                                .build()))
                .security(List.of(Map.of(BASIC_AUTH_SCHEME_NAME, List.of())))
                .build();
    }

    protected static String getEncodedCredentials(String username, String password) {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    protected Task_v0_3 sendMessageAndGetTask(Client_v0_3 client, String messageText) throws Exception {
        Message_v0_3 message = new Message_v0_3.Builder()
                .messageId(UUID.randomUUID().toString())
                .role(Message_v0_3.Role.USER)
                .parts(new TextPart_v0_3("a2a-local:" + messageText))
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Task_v0_3> receivedTask = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        client.sendMessage(message, List.of((event, agentCard) -> {
            if (event instanceof TaskEvent_v0_3 te) {
                receivedTask.set(te.getTask());
                if (te.getTask().status().state() == TaskState_v0_3.COMPLETED) {
                    latch.countDown();
                }
            } else if (event instanceof TaskUpdateEvent_v0_3 tue) {
                receivedTask.set(tue.getTask());
                if (tue.getTask().status().state() == TaskState_v0_3.COMPLETED) {
                    latch.countDown();
                }
            }
        }), error -> {
            errorRef.set(error);
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Task should complete within timeout");
        assertNull(errorRef.get(), "Should not have received an error: " + errorRef.get());

        Task_v0_3 task = receivedTask.get();
        assertNotNull(task, "Should have received a task");
        assertEquals(TaskState_v0_3.COMPLETED, task.status().state());
        return task;
    }

    @Test
    public void testOwnerCanGetOwnTask() throws Exception {
        Client_v0_3 clientA = createClient(USER_A, USER_A_PASSWORD);
        Task_v0_3 task = sendMessageAndGetTask(clientA, "owner-get-test");

        Task_v0_3 retrieved = clientA.getTask(new TaskQueryParams_v0_3(task.id()));
        assertNotNull(retrieved);
        assertEquals(task.id(), retrieved.id());
    }

    @Test
    public void testNonOwnerCannotGetTask() throws Exception {
        Client_v0_3 clientA = createClient(USER_A, USER_A_PASSWORD);
        Task_v0_3 task = sendMessageAndGetTask(clientA, "non-owner-get-test");

        Client_v0_3 clientB = createClient(USER_B, USER_B_PASSWORD);
        A2AClientException_v0_3 error = assertThrows(A2AClientException_v0_3.class, () ->
                clientB.getTask(new TaskQueryParams_v0_3(task.id())));
        assertInstanceOf(TaskNotFoundError_v0_3.class, error.getCause());
    }

    @Test
    public void testOwnerCanCancelOwnTask() throws Exception {
        Client_v0_3 clientA = createClient(USER_A, USER_A_PASSWORD);
        Task_v0_3 task = sendMessageAndGetTask(clientA, "owner-cancel-test");

        try {
            clientA.cancelTask(new TaskIdParams_v0_3(task.id()));
        } catch (A2AClientException_v0_3 e) {
            // TaskNotCancelableError is acceptable: task completed before cancel arrived
            if (!(e.getCause() instanceof TaskNotCancelableError_v0_3)) {
                fail("Owner received unexpected error when canceling own task: " + e.getCause());
            }
        }
    }

    @Test
    public void testNonOwnerCannotCancelTask() throws Exception {
        Client_v0_3 clientA = createClient(USER_A, USER_A_PASSWORD);
        Task_v0_3 task = sendMessageAndGetTask(clientA, "non-owner-cancel-test");

        Client_v0_3 clientB = createClient(USER_B, USER_B_PASSWORD);
        A2AClientException_v0_3 error = assertThrows(A2AClientException_v0_3.class, () ->
                clientB.cancelTask(new TaskIdParams_v0_3(task.id())));
        assertInstanceOf(TaskNotFoundError_v0_3.class, error.getCause());
    }

    @Test
    public void testUnauthorizedLooksLikeNotFound() throws Exception {
        Client_v0_3 clientA = createClient(USER_A, USER_A_PASSWORD);
        Task_v0_3 task = sendMessageAndGetTask(clientA, "info-hiding-test");

        Client_v0_3 clientB = createClient(USER_B, USER_B_PASSWORD);

        A2AClientException_v0_3 unauthorizedError = assertThrows(A2AClientException_v0_3.class, () ->
                clientB.getTask(new TaskQueryParams_v0_3(task.id())));
        A2AClientException_v0_3 notFoundError = assertThrows(A2AClientException_v0_3.class, () ->
                clientB.getTask(new TaskQueryParams_v0_3(UUID.randomUUID().toString())));

        assertInstanceOf(TaskNotFoundError_v0_3.class, unauthorizedError.getCause());
        assertInstanceOf(TaskNotFoundError_v0_3.class, notFoundError.getCause());
    }

    @Test
    public void testBothUsersCanCreateTasks() throws Exception {
        Client_v0_3 clientA = createClient(USER_A, USER_A_PASSWORD);
        Task_v0_3 taskA = sendMessageAndGetTask(clientA, "create-test-a");
        assertNotNull(taskA.id());

        Client_v0_3 clientB = createClient(USER_B, USER_B_PASSWORD);
        Task_v0_3 taskB = sendMessageAndGetTask(clientB, "create-test-b");
        assertNotNull(taskB.id());
    }
}
