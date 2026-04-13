package org.a2aproject.sdk.compat03.tck.server;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.compat03.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.compat03.server.agentexecution.RequestContext;
import org.a2aproject.sdk.compat03.server.events.EventQueue;
import org.a2aproject.sdk.compat03.server.tasks.TaskUpdater;
import org.a2aproject.sdk.compat03.spec.JSONRPCError;
import org.a2aproject.sdk.compat03.spec.Task;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError;
import org.a2aproject.sdk.compat03.spec.TaskState;
import org.a2aproject.sdk.compat03.spec.TaskStatus;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent;

@ApplicationScoped
public class AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new FireAndForgetAgentExecutor();
    }
    
    private static class FireAndForgetAgentExecutor implements AgentExecutor {
        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            Task task = context.getTask();

            if (task == null) {
                task = new Task.Builder()
                        .id(context.getTaskId())
                        .contextId(context.getContextId())
                        .status(new TaskStatus(TaskState.SUBMITTED))
                        .history(context.getMessage())
                        .build();
                eventQueue.enqueueEvent(task);
            }

            // Sleep to allow task state persistence before TCK resubscribe test
            if (context.getMessage().getMessageId().startsWith("test-resubscribe-message-id")) {
                int timeoutMs = Integer.parseInt(System.getenv().getOrDefault("RESUBSCRIBE_TIMEOUT_MS", "3000"));
                System.out.println("====> task id starts with test-resubscribe-message-id, sleeping for " + timeoutMs + " ms");
                try {
                    Thread.sleep(timeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            TaskUpdater updater = new TaskUpdater(context, eventQueue);

            // Immediately set to WORKING state
            updater.startWork();
            System.out.println("====> task set to WORKING, starting background execution");
            
            // Method returns immediately - task continues in background
            System.out.println("====> execute() method returning immediately, task running in background");
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            System.out.println("====> task cancel request received");
            Task task = context.getTask();

            if (task.getStatus().state() == TaskState.CANCELED) {
                System.out.println("====> task already canceled");
                throw new TaskNotCancelableError();
            }
            
            if (task.getStatus().state() == TaskState.COMPLETED) {
                System.out.println("====> task already completed");
                throw new TaskNotCancelableError();
            }

            TaskUpdater updater = new TaskUpdater(context, eventQueue);
            updater.cancel();
            eventQueue.enqueueEvent(new TaskStatusUpdateEvent.Builder()
                    .taskId(task.getId())
                    .contextId(task.getContextId())
                    .status(new TaskStatus(TaskState.CANCELED))
                    .isFinal(true)
                    .build());
            
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