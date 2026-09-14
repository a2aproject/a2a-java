package org.a2aproject.sdk.compat03.client.adapter.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.junit.jupiter.api.Test;

class RestCompat03ClientTransportTest {
    @Test
    void providerTargetsHttpJsonAndOrdinaryRestConfiguration() {
        RestCompat03ClientTransportProvider provider = new RestCompat03ClientTransportProvider();

        assertEquals("HTTP+JSON", provider.protocolBinding());
        assertEquals("0.3", provider.protocolVersion());
        assertEquals(RestTransport.class, provider.configuredTransportClass());
    }
}
