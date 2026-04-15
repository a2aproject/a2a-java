package org.a2aproject.sdk.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration test that verifies the JPA TaskStore works correctly
 * with the full client-server flow using the Client API.
 */
@QuarkusTest
public class JpaDatabaseTaskStoreIntegrationTest {

    @Inject
    TaskStore taskStore;

    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    private Client client;

    @BeforeEach
    public void setup() throws A2AClientException {
        ClientConfig clientConfig = new ClientConfig.Builder()
            .setStreaming(false)
            .build();
            
        client = Client.builder(agentCard)
            .clientConfig(clientConfig)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
            .build();
    }

    @Test
    public void testIsJpaDatabaseTaskStore() {
        assertInstanceOf(JpaDatabaseTaskStore.class, taskStore);
    }

    @Test
    public void testJpaDatabaseTaskStore() throws Exception {
        // Send a message creating the Task (no client-provided taskId — server generates it)
        Message userMessage = Message.builder()
            .role(Message.Role.ROLE_USER)
            .parts(Collections.singletonList(new TextPart("create")))
            .messageId("test-msg-1")
            .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Task> taskRef = new AtomicReference<>();

        java.util.function.BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
            if (event instanceof TaskEvent taskEvent) {
                taskRef.set(taskEvent.getTask());
                latch.countDown();
            }
        };

        client.sendMessage(userMessage, List.of(consumer), (Throwable e) -> {
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timeout waiting for task creation");
        Task createdTask = taskRef.get();
        assertNotNull(createdTask);
        assertEquals(0, createdTask.artifacts().size());
        assertEquals(TaskState.TASK_STATE_SUBMITTED, createdTask.status().state());

        final String taskId = createdTask.id();
        final String contextId = createdTask.contextId();

        // Send a message updating the Task
        userMessage = Message.builder()
            .role(Message.Role.ROLE_USER)
            .parts(Collections.singletonList(new TextPart("add-artifact")))
            .taskId(taskId)
            .messageId("test-msg-2")
            .contextId(contextId)
            .build();

        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<Task> taskRef2 = new AtomicReference<>();

        consumer = (event, card) -> {
            if (event instanceof TaskEvent taskEvent) {
                taskRef2.set(taskEvent.getTask());
                latch2.countDown();
            }
        };

        client.sendMessage(userMessage, List.of(consumer), (Throwable e) -> {
            latch2.countDown();
        });

        assertTrue(latch2.await(10, TimeUnit.SECONDS), "Timeout waiting for task creation");
        Task updatedTask = taskRef2.get();
        assertNotNull(updatedTask);
        assertEquals(1, updatedTask.artifacts().size());
        assertEquals(TaskState.TASK_STATE_SUBMITTED, updatedTask.status().state());

        Task retrievedTask = client.getTask(new TaskQueryParams(taskId), null);
        assertNotNull(retrievedTask);
        assertEquals(1, retrievedTask.artifacts().size());
        assertEquals(TaskState.TASK_STATE_SUBMITTED, retrievedTask.status().state());

        // Cancel the task
        Task cancelledTask = client.cancelTask(new CancelTaskParams(taskId), null);
        assertNotNull(cancelledTask);
        assertEquals(1, cancelledTask.artifacts().size());
        assertEquals(TaskState.TASK_STATE_CANCELED, cancelledTask.status().state());

        Task retrievedCancelledTask = client.getTask(new TaskQueryParams(taskId), null);
        assertNotNull(retrievedCancelledTask);
        assertEquals(1, retrievedCancelledTask.artifacts().size());
        assertEquals(TaskState.TASK_STATE_CANCELED, retrievedCancelledTask.status().state());

        // None of the framework code deletes tasks, so just do this manually
        taskStore.delete(taskId);
    }
}
