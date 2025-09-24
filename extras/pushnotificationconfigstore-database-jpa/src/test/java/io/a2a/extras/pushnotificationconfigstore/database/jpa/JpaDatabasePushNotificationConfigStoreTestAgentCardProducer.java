package io.a2a.extras.pushnotificationconfigstore.database.jpa;

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
 * It declares that the agent supports push notifications.
 */
@ApplicationScoped
@IfBuildProfile("test")
public class JpaDatabasePushNotificationConfigStoreTestAgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                .name("JPA PushNotificationConfigStore Integration Test Agent")
                .description("Test agent for verifying JPA PushNotificationConfigStore integration")
                .version("1.0.0")
                .url("http://localhost:8081") // Port is managed by QuarkusTest
                .preferredTransport(TransportProtocol.JSONRPC.asString())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(new AgentCapabilities.Builder()
                        .pushNotifications(true) // Enable push notifications
                        .streaming(true) // Enable streaming for automatic push notifications
                        .build())
                .skills(List.of())
                .build();
    }
}
