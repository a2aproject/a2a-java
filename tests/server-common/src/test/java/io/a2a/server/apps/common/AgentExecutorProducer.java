package io.a2a.server.apps.common;

import static io.a2a.server.ServerCallContext.TRANSPORT_KEY;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.ServerCallContext;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.A2AError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import io.a2a.spec.TransportProtocol;
import io.a2a.spec.UnsupportedOperationError;
import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("test")
public class AgentExecutorProducer {

    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                String taskId = context.getTaskId();

                // Agent-to-agent communication test
                if (taskId != null && taskId.startsWith("agent-to-agent-test")) {
                    handleAgentToAgentTest(context, agentEmitter);
                    return;
                }

                // Special handling for multi-event test
                if (taskId != null && taskId.startsWith("multi-event-test")) {
                    // First call: context.getTask() == null (new task)
                    if (context.getTask() == null) {
                        agentEmitter.startWork();
                        // Return immediately - queue stays open because task is in WORKING state
                        return;
                    } else {
                        // Second call: context.getTask() != null (existing task)
                        agentEmitter.addArtifact(
                            List.of(new TextPart("Second message artifact")),
                            "artifact-2", "Second Artifact", null);
                        agentEmitter.complete();
                        return;
                    }
                }

                // Special handling for input-required test
                if (taskId != null && taskId.startsWith("input-required-test")) {
                    // First call: context.getTask() == null (new task)
                    if (context.getTask() == null) {
                        agentEmitter.startWork();
                        agentEmitter.requiresInput(agentEmitter.newAgentMessage(
                                List.of(new TextPart("Please provide additional information")),
                                context.getMessage().metadata()));
                        // Return immediately - queue stays open because task is in INPUT_REQUIRED state
                        return;
                    } else {
                        String input = extractTextFromMessage(context.getMessage());
                        if(! "User input".equals(input)) {
                            throw new InvalidParamsError("We didn't get the expected input");
                        }
                        // Second call: context.getTask() != null (input provided)
                        agentEmitter.startWork();
                        agentEmitter.complete();
                        return;
                    }
                }

                if (context.getTaskId().equals("task-not-supported-123")) {
                    throw new UnsupportedOperationError();
                }

                // Check for delegated agent-to-agent messages (marked with special prefix)
                if (context.getMessage() != null) {
                    String userInput = extractTextFromMessage(context.getMessage());
                    if (userInput.startsWith("#a2a-delegated#")) {
                        // This is a delegated message from agent-to-agent test - complete it
                        String actualContent = userInput.substring("#a2a-delegated#".length());
                        agentEmitter.startWork();
                        String response = "Handled locally: " + actualContent;
                        agentEmitter.addArtifact(List.of(new TextPart(response)));
                        agentEmitter.complete();
                        return;
                    }
                }

                // Default handler: echo back message or task
                if (context.getMessage() != null) {
                    agentEmitter.sendMessage(context.getMessage());
                } else {
                    agentEmitter.addTask(context.getTask());
                }
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                if (context.getTask().id().equals("cancel-task-123")) {
                    agentEmitter.cancel();
                } else if (context.getTask().id().equals("cancel-task-not-supported-123")) {
                    throw new UnsupportedOperationError();
                }
            }

            /**
             * Handles agent-to-agent communication testing.
             * Detects "delegate:" prefix and forwards requests to another agent via client.
             */
            private void handleAgentToAgentTest(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                try {
                    // Get transport protocol from ServerCallContext
                    ServerCallContext callContext = context.getCallContext();
                    if (callContext == null) {
                        agentEmitter.fail(new InternalError("No call context available for agent-to-agent test"));
                        return;
                    }

                    TransportProtocol transportProtocol = (TransportProtocol) callContext.getState().get(TRANSPORT_KEY);
                    if (transportProtocol == null) {
                        agentEmitter.fail(new InternalError("Transport type not set in call context"));
                        return;
                    }

                    String serverUrl = getServerUrl(transportProtocol);

                    // Extract user message
                    String userInput = context.getUserInput("\n");

                    // Check for delegation pattern
                    if (userInput.startsWith("delegate:")) {
                        handleDelegation(userInput, transportProtocol, serverUrl, agentEmitter);
                    } else {
                        handleLocally(userInput, agentEmitter);
                    }
                } catch (Exception e) {
                    agentEmitter.fail(new InternalError("Agent-to-agent test failed: " + e.getMessage()));
                }
            }

            /**
             * Handles delegation by forwarding to another agent via client.
             */
            private void handleDelegation(String userInput, TransportProtocol transportProtocol,
                                          String serverUrl, AgentEmitter agentEmitter) {
                // Strip "delegate:" prefix
                String delegatedContent = userInput.substring("delegate:".length()).trim();

                // Create client for same transport
                try (Client client = AgentToAgentClientFactory.createClient(agentCard, transportProtocol, serverUrl)) {
                    agentEmitter.startWork();

                    // Set up consumer to capture task result
                    CountDownLatch latch = new CountDownLatch(1);
                    AtomicReference<Task> resultRef = new AtomicReference<>();
                    AtomicReference<Throwable> errorRef = new AtomicReference<>();

                    BiConsumer<ClientEvent, AgentCard> consumer = (event, agentCard) -> {
                        Task task = null;
                        if (event instanceof TaskEvent taskEvent) {
                            task = taskEvent.getTask();
                        } else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                            task = taskUpdateEvent.getTask();
                        }

                        if (task != null && task.status().state().isFinal()) {
                            resultRef.set(task);
                            latch.countDown();
                        }
                    };

                    // Delegate to another agent (new task on same server)
                    // Add a marker so the receiving agent knows to complete the task
                    Message delegatedMessage = A2A.toUserMessage("#a2a-delegated#" + delegatedContent);
                    client.sendMessage(delegatedMessage, List.of(consumer), error -> {
                        errorRef.set(error);
                        latch.countDown();
                    });

                    // Wait for response
                    if (!latch.await(30, TimeUnit.SECONDS)) {
                        agentEmitter.fail(new InternalError("Timeout waiting for delegated response"));
                        return;
                    }

                    Task delegatedResult = resultRef.get();

                    // Check for error only if we didn't get a successful result
                    // (errors can occur after completion due to stream cleanup)
                    if (delegatedResult == null && errorRef.get() != null) {
                        agentEmitter.fail(new InternalError("Delegation failed: " + errorRef.get().getMessage()));
                        return;
                    }

                    if (delegatedResult == null) {
                        agentEmitter.fail(new InternalError("No result received from delegation"));
                        return;
                    }

                    // Extract artifacts from delegated task and add to current task
                    // NOTE: We cannot use emitter.addTask(delegatedResult) because it has a different taskId
                    if (delegatedResult.artifacts() != null && !delegatedResult.artifacts().isEmpty()) {
                        for (Artifact artifact : delegatedResult.artifacts()) {
                            agentEmitter.addArtifact(artifact.parts());
                        }
                    }

                    // Complete current task
                    agentEmitter.complete();
                } catch (A2AClientException e) {
                    agentEmitter.fail(new InternalError("Failed to create client: " + e.getMessage()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    agentEmitter.fail(new InternalError("Interrupted while waiting for response"));
                }
            }

            /**
             * Handles request locally without delegation.
             */
            private void handleLocally(String userInput, AgentEmitter agentEmitter) {
                agentEmitter.startWork();
                String response = "Handled locally: " + userInput;
                agentEmitter.addArtifact(List.of(new TextPart(response)));
                agentEmitter.complete();
            }
        };
    }

    /**
     * Extract the content of TextPart in a message to create a single String.
     * @param message the message containing the TextPart.
     * @return a String aggreagating all the TextPart contents of the message.
     */
    private String extractTextFromMessage(final Message message) {
        final StringBuilder textBuilder = new StringBuilder();
        if (message.parts() != null) {
            for (final Part part : message.parts()) {
                if (part instanceof TextPart textPart) {
                    textBuilder.append(textPart.text());
                }
            }
        }
        return textBuilder.toString();
    }

    /**
     * Gets the server URL for testing based on transport protocol.
     * Uses the same port property as AgentCardProducer.
     *
     * @param transportProtocol the transport protocol
     * @return server URL (e.g., "http://localhost:8081" or "localhost:9090")
     */
    private static String getServerUrl(TransportProtocol transportProtocol) {
        // Use same property as AgentCardProducer
        String port = System.getProperty("test.agent.card.port", "8081");

        // Construct URL using same logic as AgentCardProducer
        if (transportProtocol == TransportProtocol.GRPC) {
            return "localhost:" + port;
        } else {
            // JSONRPC and HTTP_JSON both use HTTP
            return "http://localhost:" + port;
        }
    }
}
