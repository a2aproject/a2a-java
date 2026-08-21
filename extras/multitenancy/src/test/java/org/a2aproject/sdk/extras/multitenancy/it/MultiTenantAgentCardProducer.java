package org.a2aproject.sdk.extras.multitenancy.it;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.a2aproject.sdk.extras.multitenancy.Tenant;
import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;

@Singleton
public class MultiTenantAgentCardProducer {

    private static final String BASE_URL = "http://localhost:8081";

    @Produces
    @PublicAgentCard
    public AgentCard publicCard() {
        return AgentCard.builder()
                .name("Multi-Tenant Test Agent")
                .description("Test agent for multitenancy integration tests")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .extendedAgentCard(true)
                        .build())
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), BASE_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), BASE_URL)))
                .build();
    }

    @Produces
    @Singleton
    @ExtendedAgentCard
    public AgentCard defaultExtendedCard() {
        return AgentCard.builder()
                .name("default-extended")
                .description("Default extended card")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().build())
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), BASE_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), BASE_URL)))
                .build();
    }

    @Produces
    @Singleton
    @Tenant("acme")
    @ExtendedAgentCard
    public AgentCard acmeExtendedCard() {
        return AgentCard.builder()
                .name("acme-extended")
                .description("Acme extended card")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().build())
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), BASE_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), BASE_URL + "/acme")))
                .build();
    }

    @Produces
    @Singleton
    @Tenant("acme")
    public AgentCard acmePublicCard() {
        return AgentCard.builder()
                .name("Acme Agent")
                .description("Acme-specific public agent card")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .extendedAgentCard(true)
                        .build())
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), BASE_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), BASE_URL + "/acme")))
                .build();
    }

    @Produces
    @Singleton
    @Tenant("beta")
    @ExtendedAgentCard
    public AgentCard betaExtendedCard() {
        return AgentCard.builder()
                .name("beta-extended")
                .description("Beta extended card")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().build())
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), BASE_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), BASE_URL + "/beta")))
                .build();
    }

    @Produces
    @Singleton
    @Tenant("beta")
    public AgentCard betaPublicCard() {
        return AgentCard.builder()
                .name("Beta Agent")
                .description("Beta-specific public agent card")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .extendedAgentCard(true)
                        .build())
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), BASE_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), BASE_URL + "/beta")))
                .build();
    }
}
