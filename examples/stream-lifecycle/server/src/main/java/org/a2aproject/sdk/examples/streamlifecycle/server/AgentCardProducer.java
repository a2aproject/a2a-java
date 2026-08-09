package org.a2aproject.sdk.examples.streamlifecycle.server;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AgentCardProducer {

    @ConfigProperty(name = "quarkus.agentcard.protocol", defaultValue = "JSONRPC")
    String protocolStr;

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("Stream Lifecycle Demo Agent")
                .description("Demonstrates TaskStreamLifecycleHook — closes all streams when 3 subscribers connect")
                .supportedInterfaces(Collections.singletonList(getAgentInterface()))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(AgentSkill.builder()
                        .id("stream_lifecycle")
                        .name("Stream lifecycle demo")
                        .description("Sends progress messages while subscribers connect and disconnect")
                        .tags(List.of("streaming", "lifecycle"))
                        .build()))
                .build();
    }

    private AgentInterface getAgentInterface() {
        TransportProtocol protocol = TransportProtocol.fromString(protocolStr);
        String url = switch (protocol) {
            case GRPC -> "localhost:9000";
            case JSONRPC, HTTP_JSON -> "http://localhost:9999";
        };
        return new AgentInterface(protocol.asString(), url);
    }
}
