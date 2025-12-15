package io.a2a.extras.taskstore.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
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
        final String taskId = "test-task-1";
        final String contextId = "contextId";

        // Send a message creating the Task
        assertNull(taskStore.get(taskId));
        Message userMessage = Message.builder()
            .role(Message.Role.USER)
            .parts(Collections.singletonList(new TextPart("create")))
            .taskId(taskId)
            .messageId("test-msg-1")
            .contextId(contextId)
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
        assertEquals(0, createdTask.getArtifacts().size());
        assertEquals(TaskState.SUBMITTED, createdTask.getStatus().state());

        // Send a message updating the Task
        userMessage = Message.builder()
            .role(Message.Role.USER)
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
        assertEquals(1, updatedTask.getArtifacts().size());
        assertEquals(TaskState.SUBMITTED, updatedTask.getStatus().state());

        Task retrievedTask = client.getTask(new TaskQueryParams(taskId), null);
        assertNotNull(retrievedTask);
        assertEquals(1, retrievedTask.getArtifacts().size());
        assertEquals(TaskState.SUBMITTED, retrievedTask.getStatus().state());

        // Cancel the task
        Task cancelledTask = client.cancelTask(new TaskIdParams(taskId), null);
        assertNotNull(cancelledTask);
        assertEquals(1, cancelledTask.getArtifacts().size());
        assertEquals(TaskState.CANCELED, cancelledTask.getStatus().state());

        Task retrievedCancelledTask = client.getTask(new TaskQueryParams(taskId), null);
        assertNotNull(retrievedCancelledTask);
        assertEquals(1, retrievedCancelledTask.getArtifacts().size());
        assertEquals(TaskState.CANCELED, retrievedCancelledTask.getStatus().state());

        // None of the framework code deletes tasks, so just do this manually
        taskStore.delete(taskId);
    }
}
