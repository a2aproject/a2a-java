package io.a2a.server.apps.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;
import io.quarkus.arc.profile.IfBuildProfile;
import org.junit.jupiter.api.Assertions;

@ApplicationScoped
@IfBuildProfile("test")
public class AgentCardProducer {

    private static final String PREFERRED_TRANSPORT = "preferred-transport";
    private static final String A2A_REQUESTHANDLER_TEST_PROPERTIES = "/a2a-requesthandler-test.properties";

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        String port = System.getProperty("test.agent.card.port", "8081");
        String preferredTransport = loadPreferredTransportFromProperties();

        AgentCard.Builder builder = new AgentCard.Builder()
                .name("test-card")
                .description("A test agent card")
                .version("1.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(new ArrayList<>())
                .protocolVersion("0.2.5")
                .supportedInterfaces(Collections.singletonList(new AgentInterface(preferredTransport, "http://localhost:" + port)));
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

