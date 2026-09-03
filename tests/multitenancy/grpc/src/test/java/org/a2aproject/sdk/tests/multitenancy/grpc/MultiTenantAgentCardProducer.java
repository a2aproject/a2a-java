package org.a2aproject.sdk.tests.multitenancy.grpc;

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
        return card("Default Agent", grpcInterfaces());
    }

    @Produces
    @Singleton
    @ExtendedAgentCard
    public AgentCard defaultExtendedCard() {
        return card("Default Agent (extended)", grpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("acme")
    public AgentCard acmePublicCard() {
        return card("Acme Agent", grpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("acme")
    @ExtendedAgentCard
    public AgentCard acmeExtendedCard() {
        return card("Acme Agent (extended)", grpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("beta")
    public AgentCard betaPublicCard() {
        return card("Beta Agent", grpcInterfaces());
    }

    @Produces
    @Singleton
    @Tenant("beta")
    @ExtendedAgentCard
    public AgentCard betaExtendedCard() {
        return card("Beta Agent (extended)", grpcInterfaces());
    }

    private List<AgentInterface> grpcInterfaces() {
        return List.of(new AgentInterface(TransportProtocol.GRPC.asString(), "localhost:" + serverPort));
    }
}
