package io.a2a.extras.taskstore.database.jpa;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Simple test AgentCard producer for our integration test.
 */
@ApplicationScoped
@IfBuildProfile("test")
public class JpaDatabaseTaskStoreTestAgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("JPA TaskStore Integration Test Agent")
                .description("Test agent for verifying JPA TaskStore integration")
                .version("1.0.0")
                .url("http://localhost:8081")
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(new AgentCapabilities.Builder().build())
                .skills(List.of())
                .build();
    }
}
