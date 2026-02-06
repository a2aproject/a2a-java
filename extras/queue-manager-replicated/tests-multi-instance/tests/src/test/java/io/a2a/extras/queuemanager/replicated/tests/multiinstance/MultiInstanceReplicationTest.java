package io.a2a.extras.queuemanager.replicated.tests.multiinstance;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TransportProtocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Multi-instance replication test that validates event queue replication
 * between two running Quarkus instances using Testcontainers.
 * 
 * Test Architecture:
 * - Shared KafkaContainer for event replication
 * - Shared PostgreSQLContainer for task persistence
 * - Two Quarkus application containers (app1 on 8081, app2 on 8082)
 * - A2A Client instances to interact with both applications
 */
@Testcontainers
public class MultiInstanceReplicationTest {

    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.6.1";
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    private static Network network;
    private static KafkaContainer kafka;
    private static PostgreSQLContainer<?> postgres;
    private static GenericContainer<?> app1;
    private static GenericContainer<?> app2;

    private static String app1Url;
    private static String app2Url;
    private static AgentCard app1Card;
    private static AgentCard app2Card;

    private Client client1;
    private Client client2;

    @BeforeAll
    public static void setup() {
        // Create a shared network for all containers
        network = Network.newNetwork();
        
        // Start Kafka container
        kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");
        kafka.start();
        
        // Start PostgreSQL container
        postgres = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withDatabaseName("a2adb")
                .withUsername("a2auser")
                .withPassword("a2apass");
        postgres.start();

        // Get Kafka bootstrap servers for app configuration
        // Kafka is accessible within the Docker network as "kafka:9092"
        String kafkaBootstrapServers = "kafka:9092";

        // PostgreSQL connection details for apps (using network alias)
        String dbUrl = "jdbc:postgresql://postgres:5432/a2adb";
        String dbUser = "a2auser";
        String dbPassword = "a2apass";

        // Build and start Quarkus app1 container
        File app1Dir = new File("../quarkus-app-1").getAbsoluteFile();
        app1 = new GenericContainer<>(
                new ImageFromDockerfile()
                        .withFileFromPath(".", app1Dir.toPath()))
                .withNetwork(network)
                .withNetworkAliases("app1")
                .withEnv("KAFKA_BOOTSTRAP_SERVERS", kafkaBootstrapServers)
                .withEnv("DATABASE_URL", dbUrl)
                .withEnv("DATABASE_USER", dbUser)
                .withEnv("DATABASE_PASSWORD", dbPassword)
                .withExposedPorts(8081)
                .waitingFor(Wait.forHttp("/q/health/ready")
                        .forPort(8081)
                        .withStartupTimeout(Duration.ofMinutes(2)));
        app1.start();

        // Build and start Quarkus app2 container
        File app2Dir = new File("../quarkus-app-2").getAbsoluteFile();
        app2 = new GenericContainer<>(
                new ImageFromDockerfile()
                        .withFileFromPath(".", app2Dir.toPath()))
                .withNetwork(network)
                .withNetworkAliases("app2")
                .withEnv("KAFKA_BOOTSTRAP_SERVERS", kafkaBootstrapServers)
                .withEnv("DATABASE_URL", dbUrl)
                .withEnv("DATABASE_USER", dbUser)
                .withEnv("DATABASE_PASSWORD", dbPassword)
                .withExposedPorts(8082)
                .waitingFor(Wait.forHttp("/q/health/ready")
                        .forPort(8082)
                        .withStartupTimeout(Duration.ofMinutes(2)));
        app2.start();

        // Store URLs for creating clients
        app1Url = "http://localhost:" + app1.getMappedPort(8081);
        app2Url = "http://localhost:" + app2.getMappedPort(8082);

        // Get AgentCards from both apps and patch URLs to use mapped ports
        try {
            AgentCard originalApp1Card = A2A.getAgentCard(app1Url);
            AgentCard originalApp2Card = A2A.getAgentCard(app2Url);

            // Rebuild AgentCards with correct URLs (mapped ports, not container ports)
            app1Card = AgentCard.builder(originalApp1Card)
                    .supportedInterfaces(Collections.singletonList(
                            new AgentInterface(TransportProtocol.JSONRPC.asString(), app1Url)
                    ))
                    .build();

            app2Card = AgentCard.builder(originalApp2Card)
                    .supportedInterfaces(Collections.singletonList(
                            new AgentInterface(TransportProtocol.JSONRPC.asString(), app2Url)
                    ))
                    .build();
        } catch (Exception e) {
            System.err.println("=== Failed to get AgentCards ===");
            System.err.println("App1 logs:");
            System.err.println(app1.getLogs());
            System.err.println("\nApp2 logs:");
            System.err.println(app2.getLogs());
            throw new RuntimeException("Failed to get AgentCards", e);
        }
    }

    @AfterAll
    public static void teardown() {
        if (app2 != null) {
            app2.stop();
        }
        if (app1 != null) {
            app1.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
        if (kafka != null) {
            kafka.stop();
        }
        if (network != null) {
            network.close();
        }
    }

    @AfterEach
    public void cleanupClients() throws Exception {
        if (client1 != null) {
            client1.close();
            client1 = null;
        }
        if (client2 != null) {
            client2.close();
            client2 = null;
        }
    }

    // Helper methods for creating A2A clients

    private Client createClient(AgentCard agentCard) throws A2AClientException {
        ClientConfig config = ClientConfig.builder()
                .setStreaming(true)
                .build();

        return Client.builder(agentCard)
                .clientConfig(config)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .build();
    }

    private Client getClient1() throws A2AClientException {
        if (client1 == null) {
            client1 = createClient(app1Card);
        }
        return client1;
    }

    private Client getClient2() throws A2AClientException {
        if (client2 == null) {
            client2 = createClient(app2Card);
        }
        return client2;
    }

    @Test
    public void testInfrastructureStartup() {
        // Verify that all containers started successfully
        assertTrue(kafka.isRunning(), "Kafka container should be running");
        assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
        assertTrue(app1.isRunning(), "App1 container should be running");
        assertTrue(app2.isRunning(), "App2 container should be running");

        assertNotNull(kafka.getBootstrapServers());
        assertNotNull(postgres.getJdbcUrl());
        assertNotNull(app1.getMappedPort(8081));
        assertNotNull(app2.getMappedPort(8082));
    }

    /**
     * Main multi-instance replication test following architect's guidance:
     * 1. Send initial message on app1 (creates task in non-final state)
     * 2. Subscribe to that task from both app1 and app2
     * 3. Send message on app1, verify both subscribers receive it
     * 4. Send message on app2, verify both subscribers receive it
     * 5. Send final message to transition task to COMPLETED
     * 6. Verify final state reflected on both subscribers
     */
    @Test
    public void testMultiInstanceEventReplication() throws Exception {
        final String taskId = "replication-test-task-" + System.currentTimeMillis();
        final String contextId = "replication-test-context";

        Throwable testFailure = null;
        try {
            // Step 1: Send initial message NON-streaming to create task
            Message initialMessage = Message.builder(A2A.toUserMessage("Initial test message"))
                    .taskId(taskId)
                .contextId(contextId)
                .build();

        // Use NON-streaming client to create the task
        // Note: app1Card has the correct URL from Testcontainers setup
        Client nonStreamingClient = Client.builder(app1Card)
                .clientConfig(ClientConfig.builder().setStreaming(false).build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .build();

        Task createdTask = null;
        try {
            nonStreamingClient.sendMessage(initialMessage, null);

            // Retrieve the task to verify it was created
            createdTask = nonStreamingClient.getTask(new TaskQueryParams(taskId), null);
            assertNotNull(createdTask, "Task should be created");

            // Task should be in a non-final state (SUBMITTED or WORKING are both valid)
            TaskState state = createdTask.status().state();
            assertTrue(state == TaskState.SUBMITTED || state == TaskState.WORKING,
                "Task should be in SUBMITTED or WORKING state, but was: " + state);
            nonStreamingClient.close();
        } catch (Exception e) {
            System.err.println("\n=== FAILED TO CREATE TASK ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("\n=== APP1 CONTAINER LOGS ===");
            System.err.println(app1.getLogs());

            System.err.println("\n=== APP2 CONTAINER LOGS ===");
            System.err.println(app2.getLogs());

            throw e;
        }

        // Step 2: Subscribe from both app1 and app2 with proper latches

        // We need to wait for at least 3 new events after resubscription:
        // 1. TaskArtifactUpdateEvent (message from app1)
        // 2. TaskArtifactUpdateEvent (message from app2)
        // 3. TaskStatusUpdateEvent(COMPLETED) (close message)
        // Note: may also receive initial TaskEvent/TaskUpdateEvent when resubscribing
        AtomicInteger app1EventCount = new AtomicInteger(0);
        AtomicInteger app2EventCount = new AtomicInteger(0);

        // Track events received
        CopyOnWriteArrayList<ClientEvent> app1Events = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<ClientEvent> app2Events = new CopyOnWriteArrayList<>();

        AtomicReference<Throwable> app1Error = new AtomicReference<>();
        AtomicReference<Throwable> app2Error = new AtomicReference<>();
        AtomicBoolean app1ReceivedInitialTask = new AtomicBoolean(false);
        AtomicBoolean app2ReceivedInitialTask = new AtomicBoolean(false);

        // App1 subscriber
        BiConsumer<ClientEvent, AgentCard> app1Subscriber = (event, card) -> {
            String eventDetail = event.getClass().getSimpleName();
            if (event instanceof io.a2a.client.TaskUpdateEvent tue) {
                eventDetail += " [" + tue.getUpdateEvent().getClass().getSimpleName();
                if (tue.getUpdateEvent() instanceof io.a2a.spec.TaskStatusUpdateEvent statusEvent) {
                    eventDetail += ", state=" + (statusEvent.status() != null ? statusEvent.status().state() : "null");
                }
                eventDetail += "]";
            } else if (event instanceof io.a2a.client.TaskEvent te) {
                eventDetail += " [state=" + (te.getTask().status() != null ? te.getTask().status().state() : "null") + "]";
            }
            System.out.println("APP1 received event: " + eventDetail);

            // Per A2A spec 3.1.6: Handle initial TaskEvent on subscribe
            if (!app1ReceivedInitialTask.get() && event instanceof io.a2a.client.TaskEvent) {
                app1ReceivedInitialTask.set(true);
                System.out.println("APP1 filtered initial TaskEvent");
                // Don't count initial TaskEvent toward expected artifact/status events
                return;
            }
            app1Events.add(event);
            int count = app1EventCount.incrementAndGet();
            System.out.println("APP1 event count now: " + count + ", event: " + eventDetail);
        };

        Consumer<Throwable> app1ErrorHandler = error -> {
            if (!isStreamClosedError(error)) {
                app1Error.set(error);
            }
        };

        // App2 subscriber
        BiConsumer<ClientEvent, AgentCard> app2Subscriber = (event, card) -> {
            String eventDetail = event.getClass().getSimpleName();
            if (event instanceof io.a2a.client.TaskUpdateEvent tue) {
                eventDetail += " [" + tue.getUpdateEvent().getClass().getSimpleName();
                if (tue.getUpdateEvent() instanceof io.a2a.spec.TaskStatusUpdateEvent statusEvent) {
                    eventDetail += ", state=" + (statusEvent.status() != null ? statusEvent.status().state() : "null");
                }
                eventDetail += "]";
            } else if (event instanceof io.a2a.client.TaskEvent te) {
                eventDetail += " [state=" + (te.getTask().status() != null ? te.getTask().status().state() : "null") + "]";
            }
            System.out.println("APP2 received event: " + eventDetail);

            // Per A2A spec 3.1.6: Handle initial TaskEvent on subscribe
            if (!app2ReceivedInitialTask.get() && event instanceof io.a2a.client.TaskEvent) {
                app2ReceivedInitialTask.set(true);
                System.out.println("APP2 filtered initial TaskEvent");
                // Don't count initial TaskEvent toward expected artifact/status events
                return;
            }
            app2Events.add(event);
            int count = app2EventCount.incrementAndGet();
            System.out.println("APP2 event count now: " + count + ", event: " + eventDetail);
        };

        Consumer<Throwable> app2ErrorHandler = error -> {
            if (!isStreamClosedError(error)) {
                app2Error.set(error);
            }
        };

        // Start subscriptions (subscribe returns void)
        getClient1().subscribeToTask(new TaskIdParams(taskId), List.of(app1Subscriber), app1ErrorHandler);
        getClient2().subscribeToTask(new TaskIdParams(taskId), List.of(app2Subscriber), app2ErrorHandler);

        // Wait for subscriptions to be established - at least one event should arrive on each
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> app1EventCount.get() >= 1 && app2EventCount.get() >= 1);

        // Step 3: Send message on app1 (should generate TaskArtifactUpdateEvent)
        int app1BeforeMsg1 = app1EventCount.get();
        int app2BeforeMsg1 = app2EventCount.get();

        Message messageFromApp1 = Message.builder(A2A.toUserMessage("Message from app1"))
                .taskId(taskId)
                .contextId(contextId)
                .build();
        getClient1().sendMessage(messageFromApp1, List.of(), null);

        // Wait for both subscribers to receive the replicated event
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> app1EventCount.get() > app1BeforeMsg1 &&
                        app2EventCount.get() > app2BeforeMsg1);

        // Step 4: Send message on app2 (should generate TaskArtifactUpdateEvent)
        int app1BeforeMsg2 = app1EventCount.get();
        int app2BeforeMsg2 = app2EventCount.get();

        Message messageFromApp2 = Message.builder(A2A.toUserMessage("Message from app2"))
                .taskId(taskId)
                .contextId(contextId)
                .build();
        getClient2().sendMessage(messageFromApp2, List.of(), null);

        // Wait for both subscribers to receive the replicated event
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> app1EventCount.get() > app1BeforeMsg2 &&
                        app2EventCount.get() > app2BeforeMsg2);

        // Step 5: Send close message (should generate TaskStatusUpdateEvent with COMPLETED)
        int app1BeforeClose = app1EventCount.get();
        int app2BeforeClose = app2EventCount.get();

        Message closeMessage = Message.builder(A2A.toUserMessage("close"))
                .taskId(taskId)
                .contextId(contextId)
                .build();
        getClient1().sendMessage(closeMessage, List.of(), null);

        // Wait for both subscribers to receive the completion event
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .until(() -> app1EventCount.get() > app1BeforeClose &&
                        app2EventCount.get() > app2BeforeClose);


        // Verify we got at least 3 new events after initial subscription (artifact1, artifact2, completed)
        assertTrue(app1Events.size() >= 3,
                "App1 should receive at least 3 events (got " + app1Events.size() + ")");
        assertTrue(app2Events.size() >= 3,
                "App2 should receive at least 3 events (got " + app2Events.size() + ")");

        // Verify no errors
        if (app1Error.get() != null) {
            throw new AssertionError("App1 subscriber error", app1Error.get());
        }
        if (app2Error.get() != null) {
            throw new AssertionError("App2 subscriber error", app2Error.get());
        }

            // Verify both received at least 3 events (could be more due to initial state events)
            assertTrue(app1Events.size() >= 3, "App1 should receive at least 3 events, got: " + app1Events.size());
            assertTrue(app2Events.size() >= 3, "App2 should receive at least 3 events, got: " + app2Events.size());
        } catch (Throwable t) {
            testFailure = t;
            throw t;
        } finally {
            // Output container logs if test failed
            if (testFailure != null) {
                System.err.println("\n========================================");
                System.err.println("TEST FAILED - Dumping container logs");
                System.err.println("========================================\n");

                dumpContainerLogs("KAFKA", kafka, 100);
                dumpContainerLogs("APP1", app1, 200);
                dumpContainerLogs("APP2", app2, 200);

                System.err.println("\n========================================");
                System.err.println("END OF CONTAINER LOGS");
                System.err.println("========================================\n");
            }
        }
    }

    /**
     * Dumps the last N lines of logs from a container to stderr.
     */
    private void dumpContainerLogs(String containerName, org.testcontainers.containers.ContainerState container, int lastLines) {
        System.err.println("\n--- " + containerName + " LOGS (last " + lastLines + " lines) ---");
        try {
            String logs = container.getLogs();
            String[] lines = logs.split("\n");
            int start = Math.max(0, lines.length - lastLines);
            for (int i = start; i < lines.length; i++) {
                System.err.println(lines[i]);
            }
        } catch (Exception e) {
            System.err.println("Failed to retrieve " + containerName + " logs: " + e.getMessage());
        }
    }

    /**
     * Checks if the error is a normal stream closure error that should be ignored.
     * HTTP/2 stream cancellation and closure are expected during cleanup.
     */
    private boolean isStreamClosedError(Throwable error) {
        if (error == null) {
            return false;
        }

        // Check for IOException which includes stream cancellation
        if (error instanceof IOException) {
            String message = error.getMessage();
            if (message != null) {
                // Filter out normal stream closure/cancellation errors
                if (message.contains("Stream closed") ||
                    message.contains("Stream") && message.contains("cancelled") ||
                    message.contains("EOF reached") ||
                    message.contains("CANCEL")) {
                    return true;
                }
            }
        }

        // Check cause recursively
        Throwable cause = error.getCause();
        if (cause != null && cause != error) {
            return isStreamClosedError(cause);
        }

        return false;
    }
}
