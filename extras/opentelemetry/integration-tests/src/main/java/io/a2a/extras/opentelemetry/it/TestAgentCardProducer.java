package io.a2a.extras.opentelemetry.it;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.Collections;
import java.util.List;


/**
 * Produces the AgentCard for integration testing.
 */
@ApplicationScoped
public class TestAgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("OpenTelemetry Test Agent")
                .description("Test agent for OpenTelemetry integration tests")
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface("JSONRPC", "http://localhost:8081")
                ))
                .version("1.0.0-TEST")
                .documentationUrl("http://example.com/test")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(AgentSkill.builder()
                        .id("echo")
                        .name("Echo")
                        .description("Echoes back the user's message")
                        .tags(Collections.singletonList("test"))
                        .examples(List.of("hello", "test message"))
                        .build()))
                .build();
    }
}
