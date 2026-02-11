package io.a2a.examples.helloworld;


import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AgentCardProducer {

    @ConfigProperty(name = "quarkus.agentcard.protocol", defaultValue="JSONRPC")
    String protocol;

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        // NOTE: Transport validation will automatically check that transports specified
        // in this AgentCard match those available on the classpath when handlers are initialized

        return AgentCard.builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .supportedInterfaces(Collections.singletonList(getAgentInterface()))
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .build())
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

    private AgentInterface getAgentInterface() {
        switch(protocol) {
            case "GRPC":
                return new AgentInterface("GRPC", "localhost:9000");
            case "HTTP+JSON":
                return new AgentInterface("HTTP+JSON", "http://localhost:9999");
            case "JSONRPC":
            default:
                return new AgentInterface("JSONRPC", "http://localhost:9999");
        }
    }
}
