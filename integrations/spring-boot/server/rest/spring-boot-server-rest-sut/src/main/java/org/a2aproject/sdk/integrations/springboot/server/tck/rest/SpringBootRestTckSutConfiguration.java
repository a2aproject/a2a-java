package org.a2aproject.sdk.integrations.springboot.server.tck.rest;

import java.util.List;

import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SpringBootRestTckSutConfiguration {

    @Bean
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("A2A Java SDK REST TCK SUT")
                .description("Spring Boot REST system-under-test for A2A TCK validation")
                .version("1.0.0")
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:9999")))
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                        .id("tck")
                        .name("TCK Conformance")
                        .description("Handles A2A TCK conformance messages")
                        .tags(List.of("tck"))
                        .build()))
                .build();
    }
}
