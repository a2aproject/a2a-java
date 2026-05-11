package org.a2aproject.sdk.compat03.tck.server;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;

/**
 * Stub producer that overrides the v1.0 DefaultProducers @DefaultBean when the
 * multi-mode profile adds v1.0 reference dependencies to the classpath.
 * This will be replaced by a proper translation layer in the future.
 */
@ApplicationScoped
public class StubAgentCardProducer {

    private static final String DEFAULT_SUT_URL = "http://localhost:9999";

    @Produces
    @PublicAgentCard
    public AgentCard createStubAgentCard() {
        return AgentCard.builder()
                .name("stub")
                .description("Stub agent card for multi-mode testing")
                .version("0.0.0")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                        .id("stub")
                        .name("stub")
                        .description("stub")
                        .tags(List.of())
                        .build()))
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), DEFAULT_SUT_URL),
                        new AgentInterface(TransportProtocol.GRPC.asString(), DEFAULT_SUT_URL),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), DEFAULT_SUT_URL)))
                .build();
    }
}
