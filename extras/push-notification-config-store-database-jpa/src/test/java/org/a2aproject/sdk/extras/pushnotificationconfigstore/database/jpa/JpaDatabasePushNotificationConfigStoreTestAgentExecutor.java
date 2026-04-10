package org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Simple test AgentExecutor that updates the task, which in turn
 * will trigger the PushNotificationSender.
 */
@IfBuildProfile("test")
@ApplicationScoped
public class JpaDatabasePushNotificationConfigStoreTestAgentExecutor {

    @Inject
    PushNotificationSender pushNotificationSender;

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                String command = getLastTextPart(context.getMessage());

                // Switch based on the command from the test client
                switch (command) {
                    case "create":
                        agentEmitter.submit();
                        break;
                    case "update":
                        // Perform a meaningful update, like adding an artifact.
                        // This state change is what will trigger the notification.
                        agentEmitter.addArtifact(List.of(new TextPart("updated-artifact")), "art-1", "test", null);
                        break;
                    default:
                        // On the first message (which might have no text), just submit.
                        agentEmitter.submit();
                        break;
                }
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                agentEmitter.cancel();
            }
        };
    }

    private String getLastTextPart(Message message) throws A2AError {
        if (message.parts() == null || message.parts().isEmpty()) {
            return "";
        }
        Part<?> part = message.parts().get(message.parts().size() - 1);
        if (part instanceof TextPart) {
            return ((TextPart) part).text();
        }
        throw new InvalidRequestError("Last part is not text");
    }
}
