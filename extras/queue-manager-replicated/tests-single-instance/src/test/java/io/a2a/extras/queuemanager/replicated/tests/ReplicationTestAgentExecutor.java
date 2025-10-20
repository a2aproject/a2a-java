package io.a2a.extras.queuemanager.replicated.tests;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
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
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {

                TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                String lastText = getLastTextPart(context.getMessage());

                switch (lastText) {
                    case "create":
                        // Submit task - this should trigger TaskStatusUpdateEvent
                        taskUpdater.submit();
                        break;
                    case "working":
                        // Move task to WORKING state without completing - keeps queue alive
                        taskUpdater.submit();
                        taskUpdater.startWork();
                        break;
                    case "complete":
                        // Complete the task - should trigger poison pill generation
                        taskUpdater.submit();
                        taskUpdater.startWork();
                        taskUpdater.addArtifact(List.of(new TextPart("Task completed")));
                        taskUpdater.complete();
                        break;
                    default:
                        throw new InvalidRequestError("Unknown command: " + lastText);
                }
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                taskUpdater.cancel();
            }
        };
    }

    private String getLastTextPart(Message message) throws JSONRPCError {
        if (message.getParts().isEmpty()) {
            throw new InvalidRequestError("No parts in message");
        }
        Part<?> part = message.getParts().get(message.getParts().size() - 1);
        if (part.getKind() == Part.Kind.TEXT) {
            return ((TextPart) part).getText();
        }
        throw new InvalidRequestError("Last part is not text");
    }
}