package org.a2aproject.sdk.extras.multitenancy.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.a2aproject.sdk.server.multitenancy.Tenant;
import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Single CDI producer for all six multi-tenant agent cards (default/acme/beta × public/extended).
 * The transport interface stamped onto every card is chosen from the {@code preferred-transport}
 * key in {@code /a2a-requesthandler-test.properties}, so one producer serves every transport module.
 *
 * <p>Router contract: per-tenant public cards carry either {@code @Tenant} alone or
 * {@code @Tenant @PublicAgentCard}; explicitly public cards take precedence. Per-tenant extended
 * cards carry {@code @Tenant @ExtendedAgentCard}.
 */
@Singleton
public class MultiTenantAgentCardProducer {

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "" + AbstractMultiTenantServerTest.TEST_PORT)
    int serverPort;

    private final String preferredTransport = loadPreferredTransport();

    @Produces
    @Singleton
    @PublicAgentCard
    public AgentCard publicCard() {
        return card("Default Agent");
    }

    @Produces
    @Singleton
    @ExtendedAgentCard
    public AgentCard defaultExtendedCard() {
        return card("default-extended");
    }

    @Produces
    @Singleton
    @Tenant("acme")
    public AgentCard acmePublicCard() {
        return card("Acme Agent");
    }

    @Produces
    @Singleton
    @Tenant("acme")
    @ExtendedAgentCard
    public AgentCard acmeExtendedCard() {
        return card("acme-extended");
    }

    @Produces
    @Singleton
    @Tenant("beta")
    public AgentCard betaPublicCard() {
        return card("Beta Agent");
    }

    @Produces
    @Singleton
    @Tenant("beta")
    @ExtendedAgentCard
    public AgentCard betaExtendedCard() {
        return card("beta-extended");
    }

    private AgentCard card(String name) {
        String url = TransportProtocol.GRPC.asString().equals(preferredTransport)
                ? "localhost:" + serverPort
                : "http://localhost:" + serverPort;
        List<AgentInterface> interfaces = List.of(new AgentInterface(preferredTransport, url));
        return AgentCard.builder()
                .name(name)
                .description(name)
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().streaming(true).extendedAgentCard(true).build())
                .skills(List.of())
                .supportedInterfaces(interfaces)
                .build();
    }

    private static String loadPreferredTransport() {
        URL url = MultiTenantAgentCardProducer.class.getResource("/a2a-requesthandler-test.properties");
        if (url == null) {
            throw new IllegalStateException("Missing /a2a-requesthandler-test.properties on the test classpath");
        }
        Properties properties = new Properties();
        try (InputStream in = url.openStream()) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read /a2a-requesthandler-test.properties", e);
        }
        String preferredTransport = properties.getProperty("preferred-transport");
        if (preferredTransport == null || preferredTransport.isBlank()) {
            throw new IllegalStateException("preferred-transport not set in /a2a-requesthandler-test.properties");
        }
        return preferredTransport;
    }
}
