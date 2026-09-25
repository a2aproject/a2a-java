package org.a2aproject.sdk.extras.multitenancy.tests;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.multitenancy.Tenant;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.spec.TextPart;

/**
 * Shared CDI producer for multi-tenant agent executors used across multitenancy integration tests.
 * Each executor echoes its tenant label as artifact text, enabling tenant-routing verification.
 */
@ApplicationScoped
public class MultiTenantAgentExecutorProducer {

    @Produces
    @ApplicationScoped
    public AgentExecutor defaultExecutor() {
        return tenantEchoExecutor(Tenants.DEFAULT_LABEL);
    }

    @Produces
    @ApplicationScoped
    @Tenant("acme")
    public AgentExecutor acmeExecutor() {
        return tenantEchoExecutor(Tenants.ACME);
    }

    @Produces
    @ApplicationScoped
    @Tenant("beta")
    public AgentExecutor betaExecutor() {
        return tenantEchoExecutor(Tenants.BETA);
    }

    private static AgentExecutor tenantEchoExecutor(String label) {
        return (context, emitter) -> {
            emitter.startWork();
            emitter.addArtifact(List.of(new TextPart(label)));
            emitter.complete();
        };
    }
}
