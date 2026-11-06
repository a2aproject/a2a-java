package org.a2aproject.sdk.examples.helloworld.server;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.TransportProtocol;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class AgentCardProducer {

    @ConfigProperty(name = "quarkus.agentcard.protocol", defaultValue = "JSONRPC")
    String protocolStr;

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
                        .extendedAgentCard(true)
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

    // Returned by GetExtendedAgentCard. No authentication is configured here, so any client can read it.
    @Produces
    @ExtendedAgentCard
    public AgentCard extendedAgentCard() {
        AgentCard publicCard = agentCard();
        List<AgentSkill> skills = new ArrayList<>(publicCard.skills());
        skills.add(AgentSkill.builder()
                .id("super_hello_world")
                .name("Returns a SUPER hello world")
                .description("A more enthusiastic hello world, only listed in the extended agent card")
                .tags(List.of("hello world", "extended"))
                .examples(List.of("super hi", "give me a super hello"))
                .build());
        return AgentCard.builder(publicCard)
                .name("Hello World Agent - Extended Edition")
                .description("The full-featured hello world agent for authenticated users")
                .skills(skills)
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
