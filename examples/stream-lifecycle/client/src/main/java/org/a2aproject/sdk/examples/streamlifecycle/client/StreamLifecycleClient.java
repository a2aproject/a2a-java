package org.a2aproject.sdk.examples.streamlifecycle.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.http.A2ACardResolver;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfig;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TextPart;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Demonstrates the TaskStreamLifecycleHook by connecting 3 subscribers to a task.
 * <p>
 * Flow:
 * <ol>
 *   <li>Subscriber 1: sends a message (creates the task, starts streaming)</li>
 *   <li>Subscriber 2: subscribes to the same task</li>
 *   <li>Both subscribers receive progress messages from the agent</li>
 *   <li>Subscriber 3: subscribes — the server hook detects 3 subscribers and closes all streams</li>
 *   <li>All subscribers see their streams end gracefully</li>
 * </ol>
 * <p>
 * Start the server first ({@code mvn quarkus:dev} in the server module), then run this client.
 * <p>
 * This class also serves as the integration test body — {@code runAndVerify(AgentCard, String)}
 * is called by the server module's {@code @QuarkusTest} for each transport protocol.
 */
public class StreamLifecycleClient {

    private static final String SERVER_URL = "http://localhost:9999";

    private static final List<ManagedChannel> grpcChannels = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        try {
            AgentCard agentCard = A2ACardResolver.builder().baseUrl(SERVER_URL).build().getAgentCard();
            System.out.println("Resolved agent card: " + agentCard.name());
            String protocol = System.getProperty("quarkus.agentcard.protocol", "JSONRPC");
            runAndVerify(agentCard, protocol);
        } finally {
            shutdownGrpcChannels();
        }
    }

    /**
     * Runs the 3-subscriber scenario and asserts correctness.
     * Called by {@code main()} for manual runs and by the server's {@code @QuarkusTest} for each protocol.
     */
    public static void runAndVerify(AgentCard agentCard, String protocol) throws Exception {
        AtomicReference<String> taskIdRef = new AtomicReference<>();
        CountDownLatch taskIdReady = new CountDownLatch(1);

        CountDownLatch sub1StreamDone = new CountDownLatch(1);
        CountDownLatch sub2StreamDone = new CountDownLatch(1);
        CountDownLatch sub3StreamDone = new CountDownLatch(1);

        CopyOnWriteArrayList<ClientEvent> sub1Events = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<ClientEvent> sub2Events = new CopyOnWriteArrayList<>();

        // --- Subscriber 1: Send a message (creates the task) ---
        Thread sub1Thread = new Thread(() -> {
            try {
                Client client = createStreamingClient(agentCard, "Sub-1", (event, card) -> {
                    sub1Events.add(event);
                    logEvent("Sub-1", event);
                    if (taskIdRef.get() == null) {
                        String id = extractTaskId(event);
                        if (id != null) {
                            taskIdRef.set(id);
                            taskIdReady.countDown();
                        }
                    }
                }, sub1StreamDone, protocol);

                System.out.println("[Sub-1] Sending message to create task...");
                client.sendMessage(A2A.toUserMessage("Start streaming demo"));
            } catch (Exception e) {
                System.out.println("[Sub-1] Error: " + e.getMessage());
            } finally {
                sub1StreamDone.countDown();
            }
        }, "subscriber-1");
        sub1Thread.start();

        assertTrue(taskIdReady.await(10, TimeUnit.SECONDS), "Task should be created");
        String taskId = taskIdRef.get();
        assertNotNull(taskId);
        System.out.println("\n=== Task created: " + taskId + " ===\n");

        // Give some time for messages to flow to subscriber 1
        Thread.sleep(1500);

        // --- Subscriber 2: Subscribe to the existing task ---
        Thread sub2Thread = new Thread(() -> {
            try {
                Client client = createStreamingClient(agentCard, "Sub-2", (event, card) -> {
                    sub2Events.add(event);
                    logEvent("Sub-2", event);
                }, sub2StreamDone, protocol);

                System.out.println("[Sub-2] Subscribing to task " + taskId + "...");
                client.subscribeToTask(new TaskIdParams(taskId));
            } catch (Exception e) {
                System.out.println("[Sub-2] Error: " + e.getMessage());
            } finally {
                sub2StreamDone.countDown();
            }
        }, "subscriber-2");
        sub2Thread.start();

        System.out.println("\n=== Subscribers 1 and 2 are active — receiving events... ===\n");
        Thread.sleep(2000);

        // --- Subscriber 3: Triggers the hook (closes all streams) ---
        Thread sub3Thread = new Thread(() -> {
            try {
                Client client = createStreamingClient(agentCard, "Sub-3", (event, card) -> {
                    logEvent("Sub-3", event);
                }, sub3StreamDone, protocol);

                System.out.println("[Sub-3] Subscribing to task " + taskId + " (will trigger stream close)...");
                client.subscribeToTask(new TaskIdParams(taskId));
            } catch (Exception e) {
                System.out.println("[Sub-3] Error: " + e.getMessage());
            } finally {
                sub3StreamDone.countDown();
            }
        }, "subscriber-3");
        sub3Thread.start();

        // Wait for all streams to close
        assertTrue(sub1StreamDone.await(30, TimeUnit.SECONDS), "Subscriber 1 stream should close");
        assertTrue(sub2StreamDone.await(30, TimeUnit.SECONDS), "Subscriber 2 stream should close");
        assertTrue(sub3StreamDone.await(30, TimeUnit.SECONDS), "Subscriber 3 stream should close");

        System.out.println("\n=== All streams closed. ===");

        // Verify events were received
        assertTrue(sub1Events.size() >= 2,
                "Subscriber 1 should have received at least a status update and one artifact, got: " + sub1Events.size());
        assertTrue(sub2Events.size() >= 1,
                "Subscriber 2 should have received at least a task snapshot, got: " + sub2Events.size());
    }

    public static void shutdownGrpcChannels() {
        for (ManagedChannel ch : grpcChannels) {
            ch.shutdownNow();
            try {
                ch.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        grpcChannels.clear();
    }

    private static Client createStreamingClient(AgentCard agentCard, String name,
                                                BiConsumer<ClientEvent, AgentCard> consumer,
                                                CountDownLatch streamDone,
                                                String protocol) throws Exception {
        ClientBuilder builder = Client.builder(agentCard)
                .addConsumer(consumer)
                .streamingErrorHandler(e -> {
                    System.out.println("[" + name + "] Stream closed.");
                    streamDone.countDown();
                })
                .clientConfig(ClientConfig.builder().setStreaming(true).build());
        configureTransport(builder, protocol);
        return builder.build();
    }

    private static void configureTransport(ClientBuilder clientBuilder, String protocol) {
        ClientTransportConfig transportConfig;
        switch (protocol) {
            case "GRPC":
                Function<String, Channel> channelFactory = url -> {
                    String target = url.replaceAll("^https?://", "");
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                    grpcChannels.add(channel);
                    return channel;
                };
                transportConfig = new GrpcTransportConfigBuilder().channelFactory(channelFactory).build();
                clientBuilder.withTransport(GrpcTransport.class, transportConfig);
                break;
            case "HTTP+JSON":
                transportConfig = new RestTransportConfig();
                clientBuilder.withTransport(RestTransport.class, transportConfig);
                break;
            case "JSONRPC":
            default:
                transportConfig = new JSONRPCTransportConfig();
                clientBuilder.withTransport(JSONRPCTransport.class, transportConfig);
                break;
        }
    }

    public static void logEvent(String subscriber, ClientEvent event) {
        if (event instanceof TaskEvent te) {
            System.out.printf("[%s] TaskEvent — state: %s, id: %s%n",
                    subscriber, te.getTask().status().state(), te.getTask().id());
        } else if (event instanceof TaskUpdateEvent tue) {
            if (tue.getUpdateEvent() instanceof TaskArtifactUpdateEvent artifact) {
                String text = extractText(artifact);
                System.out.printf("[%s] ArtifactEvent — %s%n", subscriber, text);
            } else {
                System.out.printf("[%s] StatusUpdate — %s%n", subscriber, tue.getTask().status().state());
            }
        } else if (event instanceof MessageEvent me) {
            String text = extractText(me.getMessage());
            System.out.printf("[%s] MessageEvent — %s%n", subscriber, text);
        }
    }

    static String extractTaskId(ClientEvent event) {
        if (event instanceof TaskEvent te) {
            return te.getTask().id();
        } else if (event instanceof TaskUpdateEvent tue) {
            Task task = tue.getTask();
            return task != null ? task.id() : null;
        }
        return null;
    }

    private static String extractText(TaskArtifactUpdateEvent artifact) {
        if (artifact.artifact() == null) {
            return "(no parts)";
        }
        return extractTextFromParts(artifact.artifact().parts());
    }

    private static String extractText(Message message) {
        return extractTextFromParts(message.parts());
    }

    private static String extractTextFromParts(List<Part<?>> parts) {
        if (parts == null) {
            return "(no parts)";
        }
        StringBuilder sb = new StringBuilder();
        for (Part<?> part : parts) {
            if (part instanceof TextPart tp) {
                sb.append(tp.text());
            }
        }
        return sb.toString();
    }
}
