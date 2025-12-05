package io.a2a.extras.queuemanager.replicated.tests.multiinstance.common;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

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
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        Task task = context.getTask();
        TaskUpdater updater = new TaskUpdater(context, eventQueue);

        // Check if message contains "close" signal
        boolean shouldClose = context.getMessage().parts().stream()
                .anyMatch(part -> part instanceof TextPart tp &&
                                 tp.text() != null &&
                                 tp.text().toLowerCase().contains("close"));

        if (shouldClose) {
            // Close the task
            updater.complete();
        } else if (task == null) {
            // First message - create task in SUBMITTED state
            updater.submit();
        } else {
            // Subsequent messages - add as artifact
            updater.addArtifact(context.getMessage().parts());
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);
        updater.cancel();
    }
}
