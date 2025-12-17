package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
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
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                String command = getLastTextPart(context.getMessage());

                // Switch based on the command from the test client
                switch (command) {
                    case "create":
                        taskUpdater.submit();
                        break;
                    case "update":
                        // Perform a meaningful update, like adding an artifact.
                        // This state change is what will trigger the notification.
                        taskUpdater.addArtifact(List.of(new TextPart("updated-artifact")), "art-1", "test", null);
                        break;
                    default:
                        // On the first message (which might have no text), just submit.
                        taskUpdater.submit();
                        break;
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
