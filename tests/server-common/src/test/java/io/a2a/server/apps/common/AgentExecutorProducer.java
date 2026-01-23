package io.a2a.server.apps.common;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.A2AError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile("test")
public class AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws A2AError {
                TaskUpdater updater = new TaskUpdater(context, eventQueue);
                String taskId = context.getTaskId();

                // Special handling for multi-event test
                if (taskId != null && taskId.startsWith("multi-event-test")) {
                    // First call: context.getTask() == null (new task)
                    if (context.getTask() == null) {
                        updater.startWork();
                        // Return immediately - queue stays open because task is in WORKING state
                        return;
                    } else {
                        // Second call: context.getTask() != null (existing task)
                        updater.addArtifact(
                            List.of(new TextPart("Second message artifact")),
                            "artifact-2", "Second Artifact", null);
                        updater.complete();
                        return;
                    }
                }

                // Special handling for input-required test
                if (taskId != null && taskId.startsWith("input-required-test")) {
                    // First call: context.getTask() == null (new task)
                    if (context.getTask() == null) {
                        updater.startWork();
                        updater.requiresInput(updater.newAgentMessage(
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
                        updater.startWork();
                        updater.complete();
                        return;
                    }
                }

                if (context.getTaskId().equals("task-not-supported-123")) {
                    eventQueue.enqueueEvent(new UnsupportedOperationError());
                }
                eventQueue.enqueueEvent(context.getMessage() != null ? context.getMessage() : context.getTask());
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws A2AError {
                if (context.getTask().id().equals("cancel-task-123")) {
                    TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                    taskUpdater.cancel();
                } else if (context.getTask().id().equals("cancel-task-not-supported-123")) {
                    throw new UnsupportedOperationError();
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
