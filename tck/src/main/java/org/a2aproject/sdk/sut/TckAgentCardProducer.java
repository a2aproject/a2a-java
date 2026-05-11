package org.a2aproject.sdk.sut;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;

/**
 * CDI producer for the TCK agent card.
 *
 * <p>Generated from Gherkin scenarios — do not edit by hand.
 */
@ApplicationScoped
public class TckAgentCardProducer {

    private static final String host = getEnvOrDefault("SUT_HOST", "localhost:9999");

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        String sutJsonRpcUrl = String.format("http://%s", host);
        String sutRestUrl = sutJsonRpcUrl;
        String sutGrpcUrl = host;
        
        return AgentCard.builder()
                .name("A2A Java SDK System Under Test (SUT)")
                .description("Auto-generated System Under Test for A2A TCK conformance")
                .version("1.0.0")
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), sutJsonRpcUrl),
                        new AgentInterface(TransportProtocol.GRPC.asString(), sutGrpcUrl),
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), sutRestUrl)))
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        AgentSkill.builder()
                                .id("tck")
                                .name("TCK Conformance")
                                .description("Handles TCK conformance test messages")
                                .tags(List.of("tck"))
                                .build()))
                .build();
    }

    private static String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
