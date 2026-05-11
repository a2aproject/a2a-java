package org.a2aproject.sdk.server.apps.common;

import static org.a2aproject.sdk.spec.TransportProtocol.GRPC;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Compat03Fields;
import org.a2aproject.sdk.spec.HTTPAuthSecurityScheme;
import org.a2aproject.sdk.spec.SecurityRequirement;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.profile.IfBuildProfile;
import org.junit.jupiter.api.Assertions;

@ApplicationScoped
@IfBuildProfile("test")
public class AgentCardProducer {

    private static final String PREFERRED_TRANSPORT = "preferred-transport";
    private static final String A2A_REQUESTHANDLER_TEST_PROPERTIES = "/a2a-requesthandler-test.properties";
    private static final String BASIC_AUTH_SCHEME_NAME = "basicAuth";

    @Inject
    @ConfigProperty(name = "test.agent.security.enabled", defaultValue = "false")
    boolean securityEnabled;

    @Produces
    @PublicAgentCard
    @ExtendedAgentCard
    public AgentCard agentCard() {
        String port = System.getProperty("test.agent.card.port", "8081");
        String preferredTransport = loadPreferredTransportFromProperties();
        String transportUrl = GRPC.toString().equals(preferredTransport) ? "localhost:" + port : "http://localhost:" + port;

        List<AgentInterface> interfaces = Collections.singletonList(new AgentInterface(preferredTransport, transportUrl));

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
                .supportedInterfaces(interfaces);

        Compat03Fields.addCompat03FieldsIfAvailable(builder, interfaces, transportUrl, preferredTransport);

        // Add security configuration if enabled (for authentication tests)
        if (securityEnabled) {
            builder.securitySchemes(java.util.Map.of(
                    BASIC_AUTH_SCHEME_NAME,
                    new HTTPAuthSecurityScheme(null, "basic", "HTTP Basic authentication")))
                   .securityRequirements(java.util.List.of(
                    SecurityRequirement.builder()
                            .scheme(BASIC_AUTH_SCHEME_NAME, java.util.List.of())
                            .build()));
        }

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

