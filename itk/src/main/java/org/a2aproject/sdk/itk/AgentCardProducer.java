package org.a2aproject.sdk.itk;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.Compat03Fields;
import org.a2aproject.sdk.spec.SecurityScheme;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class AgentCardProducer {

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "10102")
    int httpPort;

    @ConfigProperty(name = "quarkus.grpc.server.port", defaultValue = "11002")
    int grpcPort;

    /**
     * The capabilities this agent advertises — everything, unless the ACTS runner asked for less.
     * See {@link ActsBehaviors#reducedCapabilities()} for why a diminished card is needed at all.
     */
    private AgentCapabilities capabilities() {
        if (ActsBehaviors.reducedCapabilities()) {
            return AgentCapabilities.builder().build();
        }
        return AgentCapabilities.builder()
                .streaming(true)
                .pushNotifications(true)
                .extendedAgentCard(true)
                .build();
    }

    // Both qualifiers on one card: ACTS exercises the extended-card endpoint for its capability
    // gating and its access controls, not for content that differs from the public card.
    @Produces
    @PublicAgentCard
    @ExtendedAgentCard
    public AgentCard agentCard() {
        String url = "http://127.0.0.1:" + httpPort;
        List<AgentInterface> interfaces = List.of(
                new AgentInterface("JSONRPC", url),
                new AgentInterface("HTTP+JSON", url),
                new AgentInterface("GRPC", "127.0.0.1:" + grpcPort));

        AgentCard.Builder builder = AgentCard.builder()
                .name("ITK Current Agent")
                .description("Java agent using A2A SDK (current source).")
                .version("1.0.0")
                .supportedInterfaces(interfaces)
                .capabilities(capabilities())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                        .id("itk_current")
                        .name("ITK Current")
                        .description("Processes ITK instruction traversals")
                        .tags(List.of("itk"))
                        .examples(List.of())
                        .build()));

        // Authentication is not a capability — AgentCapabilities has no member for it — so it is
        // declared at the top level, where the ACTS authentication precondition reads both of
        // these. Set only when enforcing, so the card never claims a scheme it does not check.
        Map<String, SecurityScheme> schemes = ActsAuth.securitySchemes();
        if (schemes != null) {
            builder.securitySchemes(schemes).securityRequirements(ActsAuth.securityRequirements());
        }

        Compat03Fields.addCompat03FieldsIfAvailable(builder, interfaces, url, "JSONRPC");

        return builder.build();
    }

    @Produces
    @PublicAgentCard
    @DefaultBean
    public AgentCard_v0_3 agentCard_v0_3() {
        String url = "http://127.0.0.1:" + httpPort;
        return new AgentCard_v0_3.Builder()
                .name("ITK Current Agent")
                .description("Java agent using A2A SDK (current source).")
                .url(url)
                .version("1.0.0")
                .preferredTransport("JSONRPC")
                .capabilities(new AgentCapabilities_v0_3(true, true, false, List.of()))
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();
    }
}
