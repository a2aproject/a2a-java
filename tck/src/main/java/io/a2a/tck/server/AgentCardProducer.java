package io.a2a.tck.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;

@ApplicationScoped
public class AgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        String sutJsonRpcUrl = getEnvOrDefault("SUT_JSONRPC_URL", "http://localhost:9999");
        String sutGrpcUrl = getEnvOrDefault("SUT_GRPC_URL", "http://localhost:9000");
        return new AgentCard.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .url(sutJsonRpcUrl)
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                                .id("hello_world")
                                .name("Returns hello world")
                                .description("just returns hello world")
                                .tags(Collections.singletonList("hello world"))
                                .examples(List.of("hi", "hello world"))
                                .build()))
                .protocolVersion("0.3.0")
                .additionalInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), sutJsonRpcUrl),
                        new AgentInterface(TransportProtocol.GRPC.asString(), sutGrpcUrl)))
                .build();
    }

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

