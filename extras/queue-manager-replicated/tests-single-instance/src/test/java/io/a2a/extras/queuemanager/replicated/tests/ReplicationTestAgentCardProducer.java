package io.a2a.extras.queuemanager.replicated.tests;


import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Produces the AgentCard for replicated queue manager integration tests.
 */
@IfBuildProfile("test")
@ApplicationScoped
public class ReplicationTestAgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("replication-test-agent")
                .description("Test agent for replicated queue manager integration testing")
                .version("1.0.0")
                .documentationUrl("http://localhost:8081/docs")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:8081")))
                .build();
    }
}