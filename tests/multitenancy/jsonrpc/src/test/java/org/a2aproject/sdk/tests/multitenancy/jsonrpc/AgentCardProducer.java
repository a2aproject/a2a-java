package org.a2aproject.sdk.tests.multitenancy.jsonrpc;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.a2aproject.sdk.extras.multitenancy.Tenant;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;

@Singleton
public class AgentCardProducer {

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8081")
    int serverPort;

    @Produces
    @Singleton
    @PublicAgentCard
    public AgentCard publicCard() {
        return card("Default Agent");
    }

    @Produces
    @Singleton
    @ExtendedAgentCard
    public AgentCard defaultExtendedCard() {
        return card("Default Agent (extended)");
    }

    @Produces
    @Singleton
    @Tenant("acme")
    public AgentCard acmePublicCard() {
        return card("Acme Agent");
    }

    @Produces
    @Singleton
    @Tenant("acme")
    @ExtendedAgentCard
    public AgentCard acmeExtendedCard() {
        return card("Acme Agent (extended)");
    }

    @Produces
    @Singleton
    @Tenant("beta")
    public AgentCard betaPublicCard() {
        return card("Beta Agent");
    }

    @Produces
    @Singleton
    @Tenant("beta")
    @ExtendedAgentCard
    public AgentCard betaExtendedCard() {
        return card("Beta Agent (extended)");
    }

    private AgentCard card(String name) {
        return AgentCard.builder()
                .name(name)
                .description(name)
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().streaming(true).extendedAgentCard(true).build())
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:" + serverPort)))
                .build();
    }
}
