package org.a2aproject.sdk.compat03.tck.server;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.Compat03Fields;

/**
 * Produces a v1.0 {@link AgentCard} derived from the v0.3 {@link AgentCard_v0_3}
 * when the multi-version profile adds v1.0 reference dependencies to the classpath.
 * Overrides the {@code @DefaultBean} in {@code DefaultProducers}.
 */
@ApplicationScoped
public class DerivedAgentCardProducer {

    @Inject
    @PublicAgentCard
    AgentCard_v0_3 v03Card;

    @Produces
    @PublicAgentCard
    public AgentCard createDerivedAgentCard() {
        List<AgentInterface> interfaces = v03Card.additionalInterfaces().stream()
                .map(iface -> new AgentInterface(iface.transport(), iface.url()))
                .toList();

        AgentCard.Builder builder = AgentCard.builder()
                .name(v03Card.name())
                .description(v03Card.description())
                .version(v03Card.version())
                .capabilities(AgentCapabilities.builder()
                        .streaming(v03Card.capabilities().streaming())
                        .pushNotifications(v03Card.capabilities().pushNotifications())
                        .build())
                .defaultInputModes(v03Card.defaultInputModes())
                .defaultOutputModes(v03Card.defaultOutputModes())
                .skills(v03Card.skills().stream()
                        .map(DerivedAgentCardProducer::toSkill10)
                        .toList())
                .supportedInterfaces(interfaces);

        Compat03Fields.addCompat03FieldsIfAvailable(builder, interfaces, v03Card.url(), v03Card.preferredTransport());

        return builder.build();
    }

    private static AgentSkill toSkill10(AgentSkill_v0_3 skill) {
        return AgentSkill.builder()
                .id(skill.id())
                .name(skill.name())
                .description(skill.description())
                .tags(skill.tags())
                .examples(skill.examples() != null ? skill.examples() : List.of())
                .build();
    }
}
