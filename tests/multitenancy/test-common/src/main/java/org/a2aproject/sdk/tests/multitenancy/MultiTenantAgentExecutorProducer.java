package org.a2aproject.sdk.tests.multitenancy;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.extras.multitenancy.Tenant;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.spec.TextPart;

/**
 * Shared CDI producer for multi-tenant agent executors used across integration tests.
 * Each executor echoes the tenant name as artifact text, enabling tenant routing verification.
 */
@ApplicationScoped
public class MultiTenantAgentExecutorProducer {

    @Produces
    @ApplicationScoped
    public AgentExecutor defaultExecutor() {
        return tenantEchoExecutor("default");
    }

    @Produces
    @ApplicationScoped
    @Tenant("acme")
    public AgentExecutor acmeExecutor() {
        return tenantEchoExecutor("acme");
    }

    @Produces
    @ApplicationScoped
    @Tenant("beta")
    public AgentExecutor betaExecutor() {
        return tenantEchoExecutor("beta");
    }

    private static AgentExecutor tenantEchoExecutor(String label) {
        return (context, emitter) -> {
            emitter.startWork();
            emitter.addArtifact(List.of(new TextPart(label)));
            emitter.complete();
        };
    }
}
