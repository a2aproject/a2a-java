package org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa;

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
 * It declares that the agent supports push notifications.
 */
@ApplicationScoped
@IfBuildProfile("test")
public class JpaDatabasePushNotificationConfigStoreTestAgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("JPA PushNotificationConfigStore Integration Test Agent")
                .description("Test agent for verifying JPA PushNotificationConfigStore integration")
                .version("1.0.0")
                .supportedInterfaces(
                        Collections.singletonList(
                                new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:8081"))) // Port is managed by QuarkusTest
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder()
                        .pushNotifications(true) // Enable push notifications
                        .streaming(true) // Enable streaming for automatic push notifications
                        .build())
                .skills(List.of())
                .build();
    }
}
