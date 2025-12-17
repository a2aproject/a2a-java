package io.a2a.server.apps.common;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.List;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
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
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                TaskUpdater updater = new TaskUpdater(context, eventQueue);
                String taskId = context.getTaskId();

                // Special handling for multi-event test
                if ("multi-event-test".equals(taskId)) {
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

                if (context.getTaskId().equals("task-not-supported-123")) {
                    eventQueue.enqueueEvent(new UnsupportedOperationError());
                }
                eventQueue.enqueueEvent(context.getMessage() != null ? context.getMessage() : context.getTask());
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
                if (context.getTask().id().equals("cancel-task-123")) {
                    TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
                    taskUpdater.cancel();
                } else if (context.getTask().id().equals("cancel-task-not-supported-123")) {
                    throw new UnsupportedOperationError();
                }
            }
        };
    }
}
