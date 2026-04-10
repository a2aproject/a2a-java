package org.a2aproject.sdk.tck.server;

import java.util.List;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;

@ApplicationScoped
public class AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new FireAndForgetAgentExecutor();
    }

    private static class FireAndForgetAgentExecutor implements AgentExecutor {

        @Override
        public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
            Task task = context.getTask();

            if (task == null) {
                if (context == null) {
                    throw new IllegalArgumentException("RequestContext  may not be null");
                }
                if (context.getTaskId() == null) {
                    throw new IllegalArgumentException("Parameter 'id' may not be null");
                }
                if (context.getContextId() == null) {
                    throw new IllegalArgumentException("Parameter 'contextId' may not be null");
                }
                task = Task.builder()
                        .id(context.getTaskId())
                        .contextId(context.getContextId())
                        .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                        .history(List.of(context.getMessage()))
                        .build();
                agentEmitter.addTask(task);
            }

            // Sleep to allow task state persistence before TCK subscribe test
            if (context.getMessage() != null && context.getMessage().messageId().startsWith("test-subscribe-message-id")) {
                int timeoutMs = Integer.parseInt(System.getenv().getOrDefault("RESUBSCRIBE_TIMEOUT_MS", "3000"));
                System.out.println("====> task id starts with test-subscribe-message-id, sleeping for " + timeoutMs + " ms");
                try {
                    Thread.sleep(timeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Immediately set to WORKING state
            agentEmitter.startWork();
            System.out.println("====> task set to WORKING, starting background execution");

            // Method returns immediately - task continues in background
            System.out.println("====> execute() method returning immediately, task running in background");
        }

        @Override
        public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
            System.out.println("====> task cancel request received");
            Task task = context.getTask();
            if (task == null) {
                System.out.println("====> No task found");
                throw new TaskNotCancelableError();
            }
            if (task.status().state() == TaskState.TASK_STATE_CANCELED) {
                System.out.println("====> task already canceled");
                throw new TaskNotCancelableError();
            }

            if (task.status().state() == TaskState.TASK_STATE_COMPLETED) {
                System.out.println("====> task already completed");
                throw new TaskNotCancelableError();
            }

            agentEmitter.cancel();
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
