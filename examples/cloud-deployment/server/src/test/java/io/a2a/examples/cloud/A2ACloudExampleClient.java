package io.a2a.examples.cloud;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TextPart;
import io.a2a.spec.TransportProtocol;

/**
 * Test client demonstrating multi-pod A2A agent deployment with modernized message protocol.
 * <p>
 * This client:
 * 1. Sends "start" to create task in SUBMITTED → WORKING state
 * 2. Creates subscription to receive streaming updates
 * 3. Sends multiple "process" messages that add artifacts showing which pod processed each
 * 4. Verifies that at least 2 different pods handled requests (proving load balancing)
 * 5. Sends "complete" to finalize task and close stream
 * <p>
 * Message Protocol:
 * - "start": Initialize task (SUBMITTED → WORKING), adds "Started by {pod-name}"
 * - "process": Add artifact "Processed by {pod-name}" (fire-and-forget, stays WORKING)
 * - "complete": Add artifact "Completed by {pod-name}" and transition to COMPLETED
 * <p>
 * Usage: Run after deploying the agent to Kubernetes and setting up port-forward:
 * kubectl port-forward -n a2a-demo svc/a2a-agent-service 8080:8080
 */
public class A2ACloudExampleClient {

    private static final String AGENT_URL = System.getProperty("agent.url", "http://localhost:8080");
    private static final int PROCESS_MESSAGE_COUNT = Integer.parseInt(System.getProperty("process.message.count", "8"));  // Number of "process" messages to send
    private static final int MESSAGE_INTERVAL_MS = Integer.parseInt(System.getProperty("message.interval.ms", "1500"));
    private static final boolean CI_MODE = Boolean.parseBoolean(System.getProperty("ci.mode", "false"));  // Early exit when 2 pods observed
    private static final int MAX_MESSAGES_UNTIL_TWO_PODS = Integer.parseInt(System.getProperty("max.messages.until.two.pods", "20"));  // Max messages before giving up
    private static final int MIN_PODS_TO_OBSERVE = 2;

    // Test state
    private final Map<String, Integer> observedPods = Collections.synchronizedMap(new HashMap<>());
    private final AtomicInteger artifactCount = new AtomicInteger(0);
    private final AtomicBoolean testFailed = new AtomicBoolean(false);
    private final CountDownLatch taskCreationLatch = new CountDownLatch(1);
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private String serverTaskId;

    // Clients
    private Client streamingClient;
    private Client nonStreamingClient;
    private ClientConfig nonStreamingConfig;

    private AgentCard agentCard;

    public static void main(String[] args) throws Exception {
        new A2ACloudExampleClient().run();
    }

    private void run() throws Exception {
        printHeader();

        agentCard = fetchAndConfigureAgentCard();
        String clientTaskId = generateClientTaskId();

        createClients(agentCard);

        sendStartMessage(clientTaskId);
        subscribeToTaskUpdates();
        sendProcessMessages();
        sendCompleteMessage();

        waitForCompletion();
        printResults();
    }

    private void printHeader() {
        System.out.println("=============================================");
        System.out.println("A2A Cloud Deployment Example Client");
        System.out.println("=============================================");
        System.out.println();
        System.out.println("Agent URL: " + AGENT_URL);
        System.out.println("Process messages: " + PROCESS_MESSAGE_COUNT);
        System.out.println("Message interval: " + MESSAGE_INTERVAL_MS + "ms");
        System.out.println();
    }

    private AgentCard fetchAndConfigureAgentCard() {
        System.out.println("Fetching agent card...");
        AgentCard fetchedCard = A2A.getAgentCard(AGENT_URL);
        System.out.println("✓ Agent: " + fetchedCard.name());
        System.out.println("✓ Description: " + fetchedCard.description());

        // Override agent card URL to use the port-forwarded URL instead of internal K8s service URL
        AgentCard agentCard = AgentCard.builder(fetchedCard)
                .supportedInterfaces(
                        Collections.singletonList(
                                // Use localhost URL for port-forwarded connection
                                new AgentInterface(TransportProtocol.JSONRPC.asString(), AGENT_URL)))
                .build();
        System.out.println();
        return agentCard;
    }

    private String generateClientTaskId() {
        String clientTaskId = "cloud-test-" + System.currentTimeMillis();
        System.out.println("Client task ID: " + clientTaskId);
        System.out.println();
        return clientTaskId;
    }

    private void createClients(AgentCard agentCard) {
        System.out.println("Creating streaming client for subscription...");
        ClientConfig streamingConfig = new ClientConfig.Builder()
                .setStreaming(true)
                .build();

        streamingClient = Client.builder(agentCard)
                .clientConfig(streamingConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();

        System.out.println("Creating non-streaming client for sending messages...");
        nonStreamingConfig = new ClientConfig.Builder()
                .setStreaming(false)
                .build();

        nonStreamingClient = Client.builder(agentCard)
                .clientConfig(nonStreamingConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();

        System.out.println("✓ Clients created");
        System.out.println();
    }

    private void sendStartMessage(String clientTaskId) {
        System.out.println("Step 1: Sending 'start' to create task...");
        Message startMessage = A2A.toUserMessage("start", clientTaskId);

        try {
            nonStreamingClient.sendMessage(startMessage, List.of((ClientEvent event, AgentCard card) -> {
                if (event instanceof TaskEvent te) {
                    serverTaskId = te.getTask().id();
                    System.out.println("✓ Task created: " + serverTaskId);
                    System.out.println("  State: " + te.getTask().status().state());
                    taskCreationLatch.countDown();
                }
            }), error -> {
                System.err.println("✗ Failed to create task: " + error.getMessage());
                testFailed.set(true);
                taskCreationLatch.countDown();
            });

            // Wait for task creation to complete (max 5 seconds)
            if (!taskCreationLatch.await(5, TimeUnit.SECONDS)) {
                System.err.println("✗ Timeout waiting for task creation");
                System.exit(1);
            }

            if (serverTaskId == null) {
                System.err.println("✗ Failed to get server task ID");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to create task: " + e.getMessage());
            System.exit(1);
        }
    }

    private void subscribeToTaskUpdates() {
        System.out.println();
        System.out.println("Step 2: Subscribing to task for streaming updates...");

        AtomicBoolean subscribed = new AtomicBoolean(false);
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 1) {
                    System.out.println("Retry attempt " + attempt + "/" + maxRetries + "...");
                    Thread.sleep(1000);  // Wait for Kafka events to propagate
                }

                streamingClient.subscribeToTask(
                        new TaskIdParams(serverTaskId),
                        List.of(this::handleSubscriptionEvent),
                        this::handleSubscriptionError
                );

                System.out.println("✓ Subscribed to task updates");
                subscribed.set(true);
                break;
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    System.out.println("⚠ Failed to subscribe (attempt " + attempt + "/" + maxRetries + "): " + e.getMessage());
                } else {
                    System.err.println("✗ Failed to subscribe after " + maxRetries + " attempts: " + e.getMessage());
                    System.exit(1);
                }
            }
        }

        if (!subscribed.get()) {
            System.err.println("✗ Failed to establish subscription");
            System.exit(1);
        }
    }

    private void handleSubscriptionEvent(ClientEvent event, AgentCard card) {
        if (event instanceof TaskUpdateEvent tue) {
            if (tue.getUpdateEvent() instanceof TaskArtifactUpdateEvent artifactEvent) {
                int count = artifactCount.incrementAndGet();
                String artifactText = extractTextFromArtifact(artifactEvent);
                System.out.println("  Artifact #" + count + ": " + artifactText);

                // Extract pod name from artifact text
                String podName = extractPodName(artifactText);
                if (podName != null && !podName.equals("unknown-pod")) {
                    int invokeCount = observedPods.getOrDefault(podName, 0);
                    observedPods.put(podName, ++invokeCount);
                    System.out.println("    → Pod: " + podName + " (Total unique pods: " + observedPods.size() + ")");
                }
            }
        } else if (event instanceof TaskEvent te) {
            // Check for task completion
            if (te.getTask().status().state().isFinal()) {
                System.out.println("  Task reached final state: " + te.getTask().status().state());
                completionLatch.countDown();
            }
        }
    }

    private void handleSubscriptionError(Throwable error) {
        // Filter out normal stream closure errors (expected when task completes)
        if (!isStreamClosedError(error)) {
            System.err.println("✗ Subscription error: " + error.getMessage());
            testFailed.set(true);
        } else {
            System.out.println("ℹ Subscription stream closed (expected after task completion)");
        }
    }

    private void sendProcessMessages() throws InterruptedException {
        System.out.println();
        if (CI_MODE) {
            System.out.println("Step 3: Sending 'process' messages until 2 pods observed (CI mode, max: " + MAX_MESSAGES_UNTIL_TWO_PODS + ", interval: " + MESSAGE_INTERVAL_MS + "ms)...");
        } else {
            System.out.println("Step 3: Sending " + PROCESS_MESSAGE_COUNT + " 'process' messages (interval: " + MESSAGE_INTERVAL_MS + "ms)...");
        }
        System.out.println("--------------------------------------------");

        int messageCount = 0;
        int maxMessages = CI_MODE ? MAX_MESSAGES_UNTIL_TWO_PODS : PROCESS_MESSAGE_COUNT;
        
        while (messageCount < maxMessages) {
            messageCount++;
            final int messageNum = messageCount;

            // Create a new client for each request to force new HTTP connection
            Client freshClient = Client.builder(agentCard)
                    .clientConfig(nonStreamingConfig)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                    .build();

            Message message = Message.builder()
                    .role(Message.Role.USER)
                    .parts(new TextPart("process"))
                    .taskId(serverTaskId)
                    .build();

            try {
                freshClient.sendMessage(message, List.of((ClientEvent event, AgentCard card) -> {
                    if (event instanceof MessageEvent || event instanceof TaskEvent) {
                        System.out.println("✓ Process message " + messageNum + " sent");
                    }
                }), error -> {
                    System.err.println("✗ Process message " + messageNum + " failed: " + error.getMessage());
                    testFailed.set(true);
                });

                Thread.sleep(MESSAGE_INTERVAL_MS);
                
                // In CI mode, check if we've observed 2 pods and can exit early
                if (CI_MODE && observedPods.size() >= 2) {
                    System.out.println();
                    System.out.println("✓ CI mode: Successfully observed 2 pods after " + messageNum + " messages. Stopping early.");
                    break;
                }
            } catch (Exception e) {
                System.err.println("✗ Failed to send process message " + messageNum + ": " + e.getMessage());
                testFailed.set(true);
            }
        }

        // Wait for process artifacts to arrive via subscription
        System.out.println();
        System.out.println("Waiting for process artifacts to arrive...");
        Thread.sleep(2000);
    }

    private void sendCompleteMessage() {
        System.out.println();
        System.out.println("Step 4: Sending 'complete' to finalize task...");

        Client completeClient = Client.builder(agentCard)
                .clientConfig(nonStreamingConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build();

        Message completeMessage = Message.builder()
                .role(Message.Role.USER)
                .parts(new TextPart("complete"))
                .taskId(serverTaskId)
                .build();

        try {
            completeClient.sendMessage(completeMessage, List.of((ClientEvent event, AgentCard card) -> {
                if (event instanceof TaskEvent te) {
                    System.out.println("✓ Complete message sent, task state: " + te.getTask().status().state());
                }
            }), error -> {
                System.err.println("✗ Failed to send complete message: " + error.getMessage());
                testFailed.set(true);
            });
        } catch (Exception e) {
            System.err.println("✗ Failed to send complete message: " + e.getMessage());
            testFailed.set(true);
        }
    }

    private void waitForCompletion() throws InterruptedException {
        System.out.println();
        System.out.println("Waiting for task to complete...");
        if (!completionLatch.await(10, TimeUnit.SECONDS)) {
            System.err.println("⚠ Timeout waiting for task completion");
        }
    }

    private void printResults() {
        System.out.println();
        System.out.println("=============================================");
        System.out.println("Test Results");
        System.out.println("=============================================");
        System.out.println("Total artifacts received: " + artifactCount.get());
        System.out.println("Unique pods observed: " + observedPods.size());
        System.out.println("Pod names and counts: " + observedPods);
        System.out.println();

        if (testFailed.get()) {
            System.out.println("✗ TEST FAILED - Errors occurred during execution");
            System.exit(1);
        } else if (observedPods.size() < MIN_PODS_TO_OBSERVE) {
            System.out.printf("✗ TEST FAILED - Expected at least %d different pods, but only saw: %d\n", MIN_PODS_TO_OBSERVE, observedPods.size());
            System.out.println("  This suggests load balancing is not working correctly.");
            System.exit(1);
        } else {
            System.out.println("✓ TEST PASSED - Successfully demonstrated multi-pod processing!");
            System.out.println("  Messages were handled by " + observedPods.size() + " different pods.");
            System.out.println("  This proves that:");
            System.out.println("    - Load balancing is working (round-robin across pods)");
            System.out.println("    - Event replication is working (subscriber sees events from all pods)");
            System.out.println("    - Database persistence is working (task state shared across pods)");
            System.exit(0);
        }
    }

    private static String extractTextFromArtifact(TaskArtifactUpdateEvent event) {
        StringBuilder text = new StringBuilder();
        if (event.artifact() != null) {
            for (Part<?> part : event.artifact().parts()) {
                if (part instanceof TextPart textPart) {
                    text.append(textPart.text());
                }
            }
        }
        return text.toString();
    }

    private static String extractPodName(String artifactText) {
        // Artifact text format: "Started by <pod-name>" or "Processed by <pod-name>" or "Completed by <pod-name>"
        if (artifactText != null) {
            if (artifactText.startsWith("Started by ")) {
                return artifactText.substring("Started by ".length()).trim();
            } else if (artifactText.startsWith("Processed by ")) {
                return artifactText.substring("Processed by ".length()).trim();
            } else if (artifactText.startsWith("Completed by ")) {
                return artifactText.substring("Completed by ".length()).trim();
            }
        }
        return null;
    }

    /**
     * Checks if the error is a normal stream closure error that should be ignored.
     * HTTP/2 stream cancellation and closure are expected when task completes and queue closes.
     * Based on MultiInstanceReplicationTest.isStreamClosedError().
     */
    private static boolean isStreamClosedError(Throwable error) {
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
