package org.a2aproject.sdk.sut;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;

import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.server.PublicAgentCard;

/**
 * Stub producer that satisfies the v0.3 handler CDI requirements when the
 * multi-version profile adds compat-0.3 dependencies to the classpath.
 * This will be replaced by a proper translation layer in the future.
 */
@ApplicationScoped
public class StubAgentCardProducer_v0_3 {

    @Produces
    @PublicAgentCard
    @DefaultBean
    public AgentCard_v0_3 createStubAgentCard() {
        return new AgentCard_v0_3.Builder()
                .name("stub")
                .description("Stub agent card for multi-version testing")
                .url("http://localhost:9999")
                .version("0.0.0")
                .capabilities(new AgentCapabilities_v0_3.Builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(new AgentSkill_v0_3.Builder()
                        .id("stub")
                        .name("stub")
                        .description("stub")
                        .tags(List.of())
                        .build()))
                .build();
    }
}
