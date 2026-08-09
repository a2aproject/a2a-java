package org.a2aproject.sdk.examples.streamlifecycle.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent executor that sends a series of messages over time, giving clients time to subscribe.
 * <p>
 * The agent sends 20 progress messages, one every 500ms. This gives the demo client enough
 * time to establish multiple subscriptions and observe events flowing to each subscriber
 * before the {@link CloseStreamsHook} closes all streams.
 * </p>
 */
@ApplicationScoped
public class AgentExecutorProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AgentExecutorProducer.class);

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                LOG.info("[AGENT] Starting execution for task {}", context.getTaskId());
                emitter.startWork();

                for (int i = 1; i <= 20; i++) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOG.info("[AGENT] Interrupted at message {}", i);
                        break;
                    }
                    String text = "Progress update " + i + "/20";
                    LOG.info("[AGENT] Sending: {}", text);
                    emitter.addArtifact(java.util.List.of(new TextPart(text)));
                }

                LOG.info("[AGENT] Completing task {}", context.getTaskId());
                emitter.complete();
            }

            @Override
            public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                throw new UnsupportedOperationError();
            }
        };
    }
}
