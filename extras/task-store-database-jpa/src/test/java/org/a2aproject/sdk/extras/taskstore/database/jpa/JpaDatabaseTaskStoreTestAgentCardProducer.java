package org.a2aproject.sdk.extras.taskstore.database.jpa;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
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
        return AgentCard.builder()
                .name("JPA TaskStore Integration Test Agent")
                .description("Test agent for verifying JPA TaskStore integration")
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().build())
                .skills(List.of())
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:8081")
                ))
                .build();
    }
}
