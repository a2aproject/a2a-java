package org.a2aproject.sdk.compat03.tck.server;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.compat03.server.PublicAgentCard;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.AgentInterface;
import org.a2aproject.sdk.compat03.spec.AgentSkill;
import org.a2aproject.sdk.compat03.spec.TransportProtocol;

@ApplicationScoped
public class AgentCardProducer {

    private static final String DEFAULT_SUT_URL = "http://localhost:9999";

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {

        String sutJsonRpcUrl = getEnvOrDefault("SUT_JSONRPC_URL", DEFAULT_SUT_URL);
        String sutGrpcUrl = getEnvOrDefault("SUT_GRPC_URL", DEFAULT_SUT_URL);
        String sutRestcUrl = getEnvOrDefault("SUT_REST_URL", DEFAULT_SUT_URL);
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
                        new AgentInterface(TransportProtocol.GRPC.asString(), sutGrpcUrl),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), sutRestcUrl)))
                .build();
    }

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

