package org.a2aproject.sdk.server.apps.common;

import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("test")
public class AgentExecutorProducer {

    // Inject the existing AgentCard to avoid special handling for grpc
    @Inject
    @PublicAgentCard
    AgentCard agentCard;

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                String taskId = context.getTaskId();
                String input = context.getMessage() != null ? extractTextFromMessage(context.getMessage()) : "";

                // Agent-to-agent communication test (routed by message content prefix)
                if (input.startsWith("delegate:") || input.startsWith("a2a-local:")) {
                    handleAgentToAgentTest(context, agentEmitter);
                    return;
                }

                // Special handling for multi-event test (routed by message content)
                if (input.startsWith("multi-event:first")) {
                    agentEmitter.startWork();
                    // Return immediately - queue stays open because task is in WORKING state
                    return;
                }
                if (input.startsWith("multi-event:second")) {
                    agentEmitter.addArtifact(
                        List.of(new TextPart("Second message artifact")),
                        "artifact-2", "Second Artifact", null);
                    agentEmitter.complete();
                    return;
                }

                // Special handling for input-required test (routed by message content)
                if (input.startsWith("input-required:")) {
                    String payload = input.substring("input-required:".length());
                    // Second call: user provided the required input - complete the task
                    if ("User input".equals(payload)) {
                        agentEmitter.complete();
                        return;
                    }
                    // First call: emit INPUT_REQUIRED
                    agentEmitter.requiresInput(agentEmitter.newAgentMessage(
                            List.of(new TextPart("Please provide additional information")),
                            context.getMessage().metadata()));
                    return;
                }

                // Special handling for auth-required test (routed by message content)
                if (input.startsWith("auth-required:")) {
                    agentEmitter.requiresAuth(agentEmitter.newAgentMessage(
                            List.of(new TextPart("Please authenticate with OAuth provider")),
                            context.getMessage().metadata()));

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InternalError("Auth simulation interrupted: " + e.getMessage());
                    }

                    agentEmitter.complete();
                    return;
                }

                if ("task-not-supported-123".equals(taskId)) {
                    throw new UnsupportedOperationError();
                }

                // Check for delegated agent-to-agent messages (marked with special prefix)
                if (input.startsWith("#a2a-delegated#")) {
                    String actualContent = input.substring("#a2a-delegated#".length());
                    agentEmitter.startWork();
                    String response = "Handled locally: " + actualContent;
                    agentEmitter.addArtifact(List.of(new TextPart(response)));
                    agentEmitter.complete();
                    return;
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

                    // Extract user message
                    String userInput = context.getUserInput("\n");
                    if (userInput == null || userInput.isEmpty()) {
                        agentEmitter.fail(new InternalError("No user input received"));
                        return;
                    }

                    // Check for delegation pattern
                    if (userInput.startsWith("delegate:")) {
                        handleDelegation(userInput, transportProtocol, agentEmitter);
                    } else if (userInput.startsWith("a2a-local:")) {
                        handleLocally(userInput.substring("a2a-local:".length()), agentEmitter);
                    } else {
                        handleLocally(userInput, agentEmitter);
                    }
                } catch (Exception e) {
                    // Log the full stack trace to help debug intermittent failures
                    e.printStackTrace();
                    agentEmitter.fail(new InternalError("Agent-to-agent test failed: " + e.getMessage()));
                }
            }

            /**
             * Handles delegation by forwarding to another agent via client.
             * <p>
             * Uses blocking client call (streaming=false) which should return the final task state
             * synchronously without requiring async callbacks and latches. This simplified approach
             * avoids race conditions between event consumption and callback invocation.
             */
            private void handleDelegation(String userInput, TransportProtocol transportProtocol,
                                          AgentEmitter agentEmitter) {
                // Strip "delegate:" prefix
                String delegatedContent = userInput.substring("delegate:".length()).trim();

                // Create client for same transport (streaming=false for blocking behavior)
                try (Client client = AgentToAgentClientFactory.createClient(agentCard, transportProtocol)) {
                    agentEmitter.startWork();

                    // Store the result task from blocking call
                    AtomicReference<Task> taskRef = new AtomicReference<>();

                    // Delegate to another agent (new task on same server)
                    // Add a marker so the receiving agent knows to complete the task
                    Message delegatedMessage = A2A.toUserMessage("#a2a-delegated#" + delegatedContent);

                    // Blocking call should return final task synchronously
                    client.sendMessage(delegatedMessage, List.of((event, card) -> {
                        if (event instanceof TaskEvent te) {
                            taskRef.set(te.getTask());
                        } else if (event instanceof TaskUpdateEvent tue) {
                            taskRef.set(tue.getTask());
                        }
                    }), null);

                    // Blocking call should have completed before returning
                    Task delegatedResult = taskRef.get();

                    if (delegatedResult == null) {
                        agentEmitter.fail(new InternalError("No result received from blocking delegation call"));
                        return;
                    }

                    // DIAGNOSTIC: Check if task is actually final
                    // If blocking call returns non-final task, it indicates a server-side race condition
                    if (!delegatedResult.status().state().isFinal()) {
                        String diagnostic = String.format(
                            "RACE CONDITION DETECTED: Blocking call returned non-final task! " +
                            "State: %s, TaskId: %s, Artifacts: %d. " +
                            "This indicates DefaultRequestHandler wait logic failed to synchronize with MainEventBusProcessor.",
                            delegatedResult.status().state(),
                            delegatedResult.id(),
                            delegatedResult.artifacts() != null ? delegatedResult.artifacts().size() : 0);
                        System.err.println(diagnostic);  // Also print to stderr for CI visibility
                        agentEmitter.fail(new InternalError(diagnostic));
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
                }
            }

            /**
             * Handles request locally without delegation.
             */
            private void handleLocally(String userInput, AgentEmitter agentEmitter) {
                try {
                    agentEmitter.startWork();
                    String response = "Handled locally: " + userInput;
                    agentEmitter.addArtifact(List.of(new TextPart(response)));
                    agentEmitter.complete();
                } catch (Exception e) {
                    // Defensive catch to ensure we always emit a final state
                    e.printStackTrace();
                    agentEmitter.fail(new InternalError("Local handling failed: " + e.getMessage()));
                }
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
}
