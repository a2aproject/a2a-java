package org.a2aproject.sdk.extras.taskstore.database.jpa;

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
 * Simple test AgentExecutor that responds to messages and uses AgentEmitter.addArtifact()
 * to trigger TaskUpdateEvents for our integration test.
 */
@IfBuildProfile("test")
@ApplicationScoped
public class JpaDatabaseTaskStoreTestAgentExecutor {

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                System.out.println("TestAgentExecutor.execute() called for task: " + context.getTaskId());
                System.out.println("Message " + context.getMessage());
                String lastText = getLastTextPart(context.getMessage());
                switch (lastText) {
                    case "create":
                        agentEmitter.submit();
                        break;
                    case "add-artifact":
                        agentEmitter.addArtifact(List.of(new TextPart(lastText)), "art-1", "test", null);
                        break;
                    default:
                        throw new InvalidRequestError(lastText + " is unknown");
                }
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                agentEmitter.cancel();
            }
        };
    }

    private String getLastTextPart(Message message) throws A2AError {
        Part<?> part = message.parts().get(message.parts().size() - 1);
        if (part instanceof TextPart) {
            return ((TextPart) part).text();
        }
        throw new InvalidRequestError("No parts");
    }
}
