package org.a2aproject.sdk.compat03.tck.server;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.util.Assert;

@ApplicationScoped
public class AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new FireAndForgetAgentExecutor();
    }

    private static class FireAndForgetAgentExecutor implements AgentExecutor {
        @Override
        public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
            Task task = context.getTask();

            if (task == null) {
                emitter.submit();
            }

            // Sleep to allow task state persistence before TCK resubscribe test
            Message message = context.getMessage();
            if (message != null && message.messageId().startsWith("test-resubscribe-message-id")) {
                int timeoutMs = Integer.parseInt(System.getenv().getOrDefault("RESUBSCRIBE_TIMEOUT_MS", "3000"));
                System.out.println("====> task id starts with test-resubscribe-message-id, sleeping for " + timeoutMs + " ms");
                try {
                    Thread.sleep(timeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Immediately set to WORKING state
            emitter.startWork();
            System.out.println("====> task set to WORKING, starting background execution");

            // Method returns immediately - task continues in background
            System.out.println("====> execute() method returning immediately, task running in background");
        }

        @Override
        public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
            System.out.println("====> task cancel request received");
            Task task = Assert.checkNotNullParam("task", context.getTask());

            if (task.status().state() == TaskState.TASK_STATE_CANCELED) {
                System.out.println("====> task already canceled");
                throw new TaskNotCancelableError();
            }

            if (task.status().state() == TaskState.TASK_STATE_COMPLETED) {
                System.out.println("====> task already completed");
                throw new TaskNotCancelableError();
            }

            emitter.cancel();

            System.out.println("====> task canceled");
        }

        /**
         * Cleanup method for proper resource management
         */
        @PreDestroy
        public void cleanup() {
            System.out.println("====> shutting down task executor");
         }
    }
}