package org.a2aproject.sdk.tests.multitenancy.jsonrpc;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.spec.TextPart;

@ApplicationScoped
public class AgentExecutorProducer {

    @Produces
    public AgentExecutor defaultExecutor() {
        return (context, emitter) -> {
            emitter.startWork();
            emitter.addArtifact(List.of(new TextPart("default")));
            emitter.complete();
        };
    }
}
