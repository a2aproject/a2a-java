package io.a2a.extras.queuemanager.replicated.tests;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
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
        return new AgentCard.Builder()
                .name("replication-test-agent")
                .description("Test agent for replicated queue manager integration testing")
                .url("http://localhost:8081")
                .version("1.0.0")
                .documentationUrl("http://localhost:8081/docs")
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .additionalInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:8081")))
                .protocolVersion("0.2.5")
                .build();
    }
}