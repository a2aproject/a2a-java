package org.a2aproject.sdk.compat03.tck.server;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

@ApplicationScoped
public class AgentCardProducer_v0_3 {

    private static final String DEFAULT_SUT_URL = "http://localhost:9999";

    @Produces
    @PublicAgentCard
    public AgentCard_v0_3 agentCard() {

        String sutJsonRpcUrl = getEnvOrDefault("SUT_JSONRPC_URL", DEFAULT_SUT_URL);
        String sutGrpcUrl = getEnvOrDefault("SUT_GRPC_URL", DEFAULT_SUT_URL);
        String sutRestcUrl = getEnvOrDefault("SUT_REST_URL", DEFAULT_SUT_URL);
        return new AgentCard_v0_3.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .url(sutJsonRpcUrl)
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill_v0_3.Builder()
                                .id("hello_world")
                                .name("Returns hello world")
                                .description("just returns hello world")
                                .tags(Collections.singletonList("hello world"))
                                .examples(List.of("hi", "hello world"))
                                .build()))
                .protocolVersion("0.3.0")
                .additionalInterfaces(List.of(
                        new AgentInterface_v0_3(TransportProtocol_v0_3.JSONRPC.asString(), sutJsonRpcUrl),
                        new AgentInterface_v0_3(TransportProtocol_v0_3.GRPC.asString(), sutGrpcUrl),
                        new AgentInterface_v0_3(TransportProtocol_v0_3.HTTP_JSON.asString(), sutRestcUrl)))
                .build();
    }

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

