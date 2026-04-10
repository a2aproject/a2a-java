package org.a2aproject.sdk.extras.queuemanager.replicated.tests;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Test AgentExecutor for replicated queue manager integration testing.
 * Handles different message types to trigger various events that should be replicated.
 */
@IfBuildProfile("test")
@ApplicationScoped
public class ReplicationTestAgentExecutor {

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                String lastText = getLastTextPart(context.getMessage());

                switch (lastText) {
                    case "create":
                        // Submit task - this should trigger TaskStatusUpdateEvent
                        agentEmitter.submit();
                        break;
                    case "working":
                        // Move task to WORKING state without completing - keeps queue alive
                        agentEmitter.submit();
                        agentEmitter.startWork();
                        break;
                    case "complete":
                        // Complete the task - should trigger poison pill generation
                        agentEmitter.submit();
                        agentEmitter.startWork();
                        agentEmitter.addArtifact(List.of(new TextPart("Task completed")));
                        agentEmitter.complete();
                        break;
                    default:
                        throw new InvalidRequestError("Unknown command: " + lastText);
                }
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                agentEmitter.cancel();
            }
        };
    }

    private String getLastTextPart(Message message) throws A2AError {
        if (message.parts().isEmpty()) {
            throw new InvalidRequestError("No parts in message");
        }
        Part<?> part = message.parts().get(message.parts().size() - 1);
        if (part instanceof TextPart) {
            return ((TextPart) part).text();
        }
        throw new InvalidRequestError("Last part is not text");
    }
}