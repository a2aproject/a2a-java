package org.a2aproject.sdk.compat03.conversion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities;
import org.a2aproject.sdk.server.PublicAgentCard;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.IfBuildProfile;

/**
 * Produces v0.3 AgentCard for compat layer tests.
 * Duplicated from v1.0 tests/server-common to avoid dependency on v1.0 test infrastructure.
 */
@ApplicationScoped
@IfBuildProfile("test")
public class AgentCardProducer {

    private static final String PREFERRED_TRANSPORT = "preferred-transport";
    private static final String PROPERTIES_FILE = "/compat-0.3-requesthandler-test.properties";

    @Produces
    @PublicAgentCard
    @DefaultBean
    public AgentCard createTestAgentCard() {
        String port = System.getProperty("test.agent.card.port", "8081");
        String preferredTransport = loadPreferredTransportFromProperties();

        // v0.3 uses 'url' field for primary endpoint
        String url = "grpc".equalsIgnoreCase(preferredTransport)
            ? "localhost:" + port
            : "http://localhost:" + port;

        return new AgentCard.Builder()
            .name("compat-0.3-test-agent")
            .description("Test agent for v0.3 compatibility layer")
            .url(url)
            .version("1.0.0")
            .preferredTransport(preferredTransport)
            .capabilities(new AgentCapabilities(true, true, true, List.of()))
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .additionalInterfaces(new ArrayList<>())
            .build();
    }

    private static String loadPreferredTransportFromProperties() {
        URL url = AgentCardProducer.class.getResource(PROPERTIES_FILE);
        if (url == null) {
            // Default to jsonrpc if no config found
            return "jsonrpc";
        }
        Properties properties = new Properties();
        try {
            try (InputStream in = url.openStream()) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test properties", e);
        }
        return properties.getProperty(PREFERRED_TRANSPORT, "jsonrpc");
    }
}
