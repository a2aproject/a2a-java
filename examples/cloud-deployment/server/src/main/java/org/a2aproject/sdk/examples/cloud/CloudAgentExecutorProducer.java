package org.a2aproject.sdk.examples.cloud;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer for the cloud deployment example agent executor.
 */
@ApplicationScoped
public class CloudAgentExecutorProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudAgentExecutorProducer.class);

    @Produces
    public AgentExecutor agentExecutor() {
        return new CloudAgentExecutor();
    }

    /**
     * Modernized agent executor demonstrating multi-pod deployment with event replication.
     *
     * Message Protocol:
     * - "start": Initialize task (SUBMITTED → WORKING), adds "Started by {pod-name}"
     * - "process": Add artifact "Processed by {pod-name}" (fire-and-forget, stays WORKING)
     * - "complete": Add artifact "Completed by {pod-name}" and transition to COMPLETED
     *
     * This demonstrates:
     * - Cross-pod event replication via Kafka
     * - Fire-and-forget pattern with controlled completion
     * - Round-robin load balancing across pods
     */
    private static class CloudAgentExecutor implements AgentExecutor {

        @Override
        public void execute(RequestContext context, AgentEmitter agentEmitter) throws A2AError {

            try {
                // Extract user message and normalize
                String messageText = extractTextFromMessage(context.getMessage()).trim().toLowerCase();
                LOGGER.info("Received message: '{}'", messageText);

                // Get pod name from environment (set by Kubernetes Downward API)
                String podName = System.getenv("POD_NAME");
                if (podName == null || podName.isEmpty()) {
                    podName = "unknown-pod";
                }
                LOGGER.info("Processing on pod: {}", podName);

                // Simulate some processing time to make cross-pod behavior more visible
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InternalError("Processing interrupted");
                }

                // Handle message based on command
                if ("complete".equals(messageText)) {
                    // Completion trigger - add final artifact and complete
                    LOGGER.info("Completion requested on pod: {}", podName);
                    String artifactText = "Completed by " + podName;
                    List<Part<?>> parts = List.of(new TextPart(artifactText));
                    agentEmitter.addArtifact(parts);
                    agentEmitter.complete();
                    LOGGER.info("Task completed on pod: {}", podName);

                } else if (context.getTask() == null) {
                    // Initial message - create task in SUBMITTED → WORKING state
                    LOGGER.info("Creating new task on pod: {}", podName);
                    agentEmitter.submit();
                    agentEmitter.startWork();
                    String artifactText = "Started by " + podName;
                    List<Part<?>> parts = List.of(new TextPart(artifactText));
                    agentEmitter.addArtifact(parts);
                    LOGGER.info("Task created and started on pod: {}", podName);

                } else {
                    // Subsequent messages - add artifacts (fire-and-forget, stays in WORKING)
                    LOGGER.info("Adding artifact on pod: {}", podName);
                    String artifactText = "Processed by " + podName;
                    List<Part<?>> parts = List.of(new TextPart(artifactText));
                    agentEmitter.addArtifact(parts);
                    // No state change - task remains in WORKING
                    LOGGER.info("Artifact added on pod: {}", podName);
                }

            } catch (A2AError e) {
                LOGGER.error("JSONRPC error processing task", e);
                throw e;
            } catch (Exception e) {
                LOGGER.error("Error processing task", e);
                throw new InternalError("Processing failed: " + e.getMessage());
            }
        }

        @Override
        public void cancel(RequestContext context, AgentEmitter agentEmitter) throws A2AError {
            LOGGER.info("Task cancellation requested");
            agentEmitter.cancel();
        }

        /**
         * Extracts text content from a message.
         */
        private String extractTextFromMessage(Message message) {
            StringBuilder textBuilder = new StringBuilder();
            if (message.parts() != null) {
                for (Part<?> part : message.parts()) {
                    if (part instanceof TextPart textPart) {
                        textBuilder.append(textPart.text());
                    }
                }
            }
            return textBuilder.toString();
        }
    }
}
