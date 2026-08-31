package org.a2aproject.sdk.tests.multitenancy;

import java.util.List;

import jakarta.enterprise.inject.Vetoed;

import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/**
 * Base helper for building multi-tenant agent cards in integration tests.
 * Not a CDI bean — consuming modules define their own transport-specific card producer.
 * Each module's card producer must declare only the interfaces whose transports are
 * deployed in that module (JSONRPC/HTTP for jsonrpc tests, GRPC for grpc tests).
 */
@Vetoed
public class MultiTenantAgentCardProducer {

    protected static AgentCard card(String name, List<AgentInterface> supportedInterfaces) {
        return AgentCard.builder()
                .name(name)
                .description(name)
                .version("1.0.0")
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .capabilities(AgentCapabilities.builder().streaming(true).extendedAgentCard(true).build())
                .skills(List.of())
                .supportedInterfaces(supportedInterfaces)
                .build();
    }
}
