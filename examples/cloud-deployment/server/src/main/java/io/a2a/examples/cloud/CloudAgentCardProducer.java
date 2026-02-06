package io.a2a.examples.cloud;


import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Producer for the cloud deployment example agent card.
 */
@ApplicationScoped
public class CloudAgentCardProducer {

    @Inject
    @ConfigProperty(name = "agent.url", defaultValue = "http://localhost:8080")
    String agentUrl;

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("Cloud Deployment Demo Agent")
                .description("Demonstrates A2A multi-pod deployment with Kafka event replication, " +
                        "PostgreSQL persistence, and round-robin load balancing across Kubernetes pods")
                .supportedInterfaces(Collections.singletonList(
                        new AgentInterface("JSONRPC", agentUrl)))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(
                        AgentSkill.builder()
                                .id("multi_pod_demo")
                                .name("Multi-Pod Replication Demo")
                                .description("Demonstrates cross-pod event replication. " +
                                        "Send 'start' to initialize, 'process' to add artifacts, " +
                                        "'complete' to finalize. Each artifact shows which pod processed it.")
                                .tags(List.of("demo", "cloud", "kubernetes", "replication"))
                                .examples(List.of(
                                        "start",
                                        "process",
                                        "complete"
                                ))
                                .build()
                ))
                .build();
    }
}
