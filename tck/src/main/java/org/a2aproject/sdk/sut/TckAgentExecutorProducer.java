package org.a2aproject.sdk.sut;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.FileWithUri;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TextPart;

/**
 * CDI producer for the TCK agent executor.
 *
 * <p>Generated from Gherkin scenarios — do not edit by hand.
 */
@ApplicationScoped
public class TckAgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                String messageId = context.getMessage().messageId();

                if (messageId.startsWith("tck-stream-artifact-chunked")) {
                    emitter.startWork();
                    emitter.addArtifact(List.of(new TextPart("chunk-1 ")), null, null, null, true, false);
                    emitter.addArtifact(List.of(new TextPart("chunk-2")), null, null, null, true, true);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("test-resubscribe-message-id")) {
                    emitter.startWork();
                    try { Thread.sleep(4000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-stream-artifact-text")) {
                    emitter.startWork();
                    emitter.addArtifact(List.of(new TextPart("Streamed text content")), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-stream-artifact-file")) {
                    emitter.startWork();
                    emitter.addArtifact(List.of(new FilePart(new FileWithBytes("text/plain", "output.txt", "dGNr"))), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-stream-ordering-001")) {
                    emitter.startWork();
                    emitter.addArtifact(List.of(new TextPart("Ordered output")), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-artifact-file-url")) {
                    emitter.addArtifact(List.of(new FilePart(new FileWithUri("text/plain", "output.txt", "https://example.com/output.txt"))), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-message-response")) {
                    emitter.sendMessage(List.of(new TextPart("Direct message response")));
                    return;
                }

                if (messageId.startsWith("tck-input-required")) {
                    emitter.requiresInput();
                    return;
                }

                if (messageId.startsWith("tck-complete-task")) {
                    emitter.complete(A2A.toAgentMessage("Hello from TCK"));
                    return;
                }

                if (messageId.startsWith("tck-artifact-text")) {
                    emitter.addArtifact(List.of(new TextPart("Generated text content")), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-artifact-file")) {
                    emitter.addArtifact(List.of(new FilePart(new FileWithBytes("text/plain", "output.txt", "dGNr"))), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-artifact-data")) {
                    emitter.addArtifact(List.of(DataPart.fromJson("{\"key\": \"value\", \"count\": 42}")), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-reject-task")) {
                    throw new A2AError(-1, "rejected", null);
                }

                if (messageId.startsWith("tck-stream-001")) {
                    emitter.startWork();
                    emitter.addArtifact(List.of(new TextPart("Stream hello from TCK")), null, null, null);
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-stream-002")) {
                    emitter.complete();
                    return;
                }

                if (messageId.startsWith("tck-stream-003")) {
                    emitter.startWork();
                    emitter.addArtifact(List.of(new TextPart("Stream task lifecycle")), null, null, null);
                    emitter.complete();
                    return;
                }

                // Default: complete the task with an echo response
                emitter.complete(A2A.toAgentMessage("Unhandled messageId prefix: " + messageId));
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                emitter.cancel();
            }
        };
    }
}
