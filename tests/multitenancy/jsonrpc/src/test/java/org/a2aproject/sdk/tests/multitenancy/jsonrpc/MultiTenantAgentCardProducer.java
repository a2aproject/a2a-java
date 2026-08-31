package org.a2aproject.sdk.tests.multitenancy.jsonrpc;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.a2aproject.sdk.extras.multitenancy.Tenant;
import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class MultiTenantAgentCardProducer extends org.a2aproject.sdk.tests.multitenancy.MultiTenantAgentCardProducer {

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8081")
    int serverPort;

    @Produces
    @Singleton
    @PublicAgentCard
    public AgentCard publicCard() {
        return card("Default Agent", jsonrpcInterfaces());
    }

    @Produces
    @Singleton
    @ExtendedAgentCard
    public AgentCard defaultExtendedCard() {
        return card("Default Agent (extended)", jsonrpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("acme")
    public AgentCard acmePublicCard() {
        return card("Acme Agent", jsonrpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("acme")
    @ExtendedAgentCard
    public AgentCard acmeExtendedCard() {
        return card("Acme Agent (extended)", jsonrpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("beta")
    public AgentCard betaPublicCard() {
        return card("Beta Agent", jsonrpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("beta")
    @ExtendedAgentCard
    public AgentCard betaExtendedCard() {
        return card("Beta Agent (extended)", jsonrpcInterfaces());
    }

    private List<AgentInterface> jsonrpcInterfaces() {
        return List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:" + serverPort));
    }
}
