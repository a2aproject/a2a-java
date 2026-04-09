package org.a2aproject.sdk.extras.queuemanager.replicated.tests.multiinstance.common;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TextPart;

/**
 * Shared test agent executor for multi-instance replication tests.
 *
 * Behavior:
 * 1. Creates task in SUBMITTED state on first message
 * 2. Adds messages as artifacts on subsequent messages
 * 3. Completes task when message contains "close"
 */
public class MultiInstanceReplicationAgentExecutor implements AgentExecutor {
    @Override
    public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
        Task task = context.getTask();

        // Check if message contains "close" signal
        boolean shouldClose = context.getMessage().parts().stream()
                .anyMatch(part -> part instanceof TextPart tp &&
                                 tp.text() != null &&
                                 tp.text().toLowerCase().contains("close"));

        if (shouldClose) {
            // Close the task
            agentEmitter.complete();
        } else if (task == null) {
            // First message - create task in SUBMITTED state
            agentEmitter.submit();
        } else {
            // Subsequent messages - add as artifact
            agentEmitter.addArtifact(context.getMessage().parts());
        }
    }

    @Override
    public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
        agentEmitter.cancel();
    }
}
