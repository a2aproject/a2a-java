package org.a2aproject.sdk.extras.multitenancy.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.extras.multitenancy.Tenant;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TextPart;

@ApplicationScoped
public class MultiTenantAgentExecutorProducer {

    @Produces
    public AgentExecutor defaultExecutor() {
        return new TenantEchoExecutor("default");
    }

    @Produces
    @Tenant("acme")
    public AgentExecutor acmeExecutor() {
        return new TenantEchoExecutor("acme");
    }

    @Produces
    @Tenant("beta")
    public AgentExecutor betaExecutor() {
        return new TenantEchoExecutor("beta");
    }

    static class TenantEchoExecutor implements AgentExecutor {
        private final String label;

        TenantEchoExecutor(String label) {
            this.label = label;
        }

        @Override
        public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
            emitter.startWork();
            emitter.addArtifact(List.of(new TextPart(label)));
            emitter.complete();
        }

        @Override
        public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
            emitter.cancel();
        }
    }
}
