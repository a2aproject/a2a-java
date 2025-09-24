package io.a2a.extras.taskstore.database.jpa;

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
 * Simple test AgentExecutor that responds to messages and uses TaskUpdater.addArtifact()
 * to trigger TaskUpdateEvents for our integration test.
 */
@IfBuildProfile("test")
@ApplicationScoped
public class JpaDatabaseTaskStoreTestAgentExecutor {

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                System.out.println("TestAgentExecutor.execute() called for task: " + context.getTaskId());
                System.out.println("Message " + context.getMessage());

                TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                String lastText = getLastTextPart(context.getMessage());
                switch (lastText) {
                    case "create":
                        taskUpdater.submit();
                        break;
                    case "add-artifact":
                        taskUpdater.addArtifact(List.of(new TextPart(lastText)), "art-1", "test", null);
                        break;
                    default:
                        throw new InvalidRequestError(lastText + " is unknown");
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
        Part<?> part = message.getParts().get(message.getParts().size() - 1);
        if (part.getKind() == Part.Kind.TEXT) {
            return ((TextPart) part).getText();
        }
        throw new InvalidRequestError("No parts");
    }
}
