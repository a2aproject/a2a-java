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
public class AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
                String taskId = context.getTaskId();

                // Special handling for multi-event test
                if ("multi-event-test".equals(taskId)) {
                    // First call: context.getTask() == null (new task)
                    if (context.getTask() == null) {
                        agentEmitter.startWork();
                        // Return immediately - queue stays open because task is in WORKING state
                        return;
                    } else {
                        // Second call: context.getTask() != null (existing task)
                        agentEmitter.addArtifact(
                            List.of(new TextPart("Second message artifact")),
                            "artifact-2", "Second Artifact", null);
                        agentEmitter.complete();
                        return;
                    }
                }

                if (context.getTaskId().equals("task-not-supported-123")) {
                    throw new UnsupportedOperationError();
                }
                
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
        };
    }
}
