package org.a2aproject.sdk.compat03.client.adapter.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

class RestCompat03ClientTransportTest {
    @Test
    void providerTargetsHttpJsonAndOrdinaryRestConfiguration() {
        RestCompat03ClientTransportProvider provider = new RestCompat03ClientTransportProvider();

        assertEquals("HTTP+JSON", provider.protocolBinding());
        assertEquals("0.3", provider.protocolVersion());
        assertEquals(RestTransport.class, provider.configuredTransportClass());
    }

    @Test
    void rejectsTenantOnLegacyAgentInterface() {
        AgentCard card = cardWithTenant();

        A2AClientException exception = assertThrows(A2AClientException.class,
                () -> new RestCompat03ClientTransportProvider().create(null, card, card.supportedInterfaces().get(0)));

        assertTrue(exception.getMessage().contains("tenant"));
    }

    private static AgentCard cardWithTenant() {
        return AgentCard.builder().name("agent").description("description").version("1")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(java.util.List.of("text")).defaultOutputModes(java.util.List.of("text"))
                .skills(java.util.List.of())
                .supportedInterfaces(java.util.List.of(new AgentInterface("HTTP+JSON", "https://example.test", "tenant", "0.3")))
                .build();
    }
}
