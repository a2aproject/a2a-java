package org.a2aproject.sdk.compat03.conversion;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("test")
public class AgentExecutorProducer_v0_3 {

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                String taskId = context.getTaskId();
                String input = context.getMessage() != null ? extractTextFromMessage(context.getMessage()) : "";

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
                        throw new org.a2aproject.sdk.spec.InternalError("Auth simulation interrupted: " + e.getMessage());
                    }

                    agentEmitter.complete();
                    return;
                }

                if ("task-not-supported-123".equals(taskId)) {
                    throw new UnsupportedOperationError();
                }

                if (context.getMessage() != null) {
                    agentEmitter.sendMessage(context.getMessage());
                } else {
                    agentEmitter.addTask(context.getTask());
                }
            }

            private String extractTextFromMessage(org.a2aproject.sdk.spec.Message message) {
                if (message.parts() == null || message.parts().isEmpty()) {
                    return "";
                }
                return message.parts().stream()
                        .filter(part -> part instanceof TextPart)
                        .map(part -> ((TextPart) part).text())
                        .findFirst()
                        .orElse("");
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                if (context.getTask().id().equals("cancel-task-123")) {
                    agentEmitter.cancel();
                } else if (context.getTask().id().equals("cancel-task-not-supported-123")) {
                    throw new UnsupportedOperationError();
                }
            }
        };
    }
}
