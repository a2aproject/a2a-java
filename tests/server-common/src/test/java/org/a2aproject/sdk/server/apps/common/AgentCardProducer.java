package org.a2aproject.sdk.server.apps.common;

import static org.a2aproject.sdk.spec.TransportProtocol.GRPC;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

import io.quarkus.arc.profile.IfBuildProfile;
import org.junit.jupiter.api.Assertions;

@ApplicationScoped
@IfBuildProfile("test")
public class AgentCardProducer {

    private static final String PREFERRED_TRANSPORT = "preferred-transport";
    private static final String A2A_REQUESTHANDLER_TEST_PROPERTIES = "/a2a-requesthandler-test.properties";

    @Produces
    @PublicAgentCard
    @ExtendedAgentCard
    public AgentCard agentCard() {
        String port = System.getProperty("test.agent.card.port", "8081");
        String preferredTransport = loadPreferredTransportFromProperties();
        String transportUrl = GRPC.toString().equals(preferredTransport) ? "localhost:" + port : "http://localhost:" + port;

        AgentCard.Builder builder = AgentCard.builder()
                .name("test-card")
                .description("A test agent card")
                .version("1.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extendedAgentCard(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(new ArrayList<>())
                .supportedInterfaces(Collections.singletonList(new AgentInterface(preferredTransport, transportUrl)));
        return builder.build();
    }

    private static String loadPreferredTransportFromProperties() {
        URL url = AgentCardProducer.class.getResource(A2A_REQUESTHANDLER_TEST_PROPERTIES);
        if (url == null) {
            return null;
        }
        Properties properties = new Properties();
        try {
            try (InputStream in = url.openStream()){
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String preferredTransport = properties.getProperty(PREFERRED_TRANSPORT);
        Assertions.assertNotNull(preferredTransport);
        return preferredTransport;
    }
}

