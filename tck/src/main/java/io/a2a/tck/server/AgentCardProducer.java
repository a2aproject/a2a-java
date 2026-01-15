package io.a2a.tck.server;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.TransportProtocol;

@ApplicationScoped
public class AgentCardProducer {

    private static final String DEFAULT_SUT_URL = "http://localhost:9999";

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {

        String sutJsonRpcUrl = getEnvOrDefault("SUT_JSONRPC_URL", DEFAULT_SUT_URL);
        String sutGrpcUrl = getEnvOrDefault("SUT_GRPC_URL", DEFAULT_SUT_URL);
        String sutRestcUrl = getEnvOrDefault("SUT_REST_URL", DEFAULT_SUT_URL);
        return AgentCard.builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), sutJsonRpcUrl),
                        new AgentInterface(TransportProtocol.GRPC.asString(), sutGrpcUrl),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), sutRestcUrl)))
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .build())
                .securitySchemes(Map.of("acme", HTTPAuthSecurityScheme.builder()
                       .scheme("basic")
                       .build()))
                .security(List.of(Map.of("acme", Collections.emptyList())))
                .supportsExtendedAgentCard(true)
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(AgentSkill.builder()
                                .id("hello_world")
                                .name("Returns hello world")
                                .description("just returns hello world")
                                .tags(Collections.singletonList("hello world"))
                                .examples(List.of("hi", "hello world"))
                                .build()))
                .build();
    }

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @Produces
    @ExtendedAgentCard
    public AgentCard extendedAgentCard() {
        return AgentCard.builder(agentCard())
                //TODO what can we put in the extended agent card
                .build();
    }
}

