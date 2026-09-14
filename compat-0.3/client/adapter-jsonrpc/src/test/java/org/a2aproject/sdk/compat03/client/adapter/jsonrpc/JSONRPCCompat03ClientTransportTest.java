package org.a2aproject.sdk.compat03.client.adapter.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.junit.jupiter.api.Test;

class JSONRPCCompat03ClientTransportTest {
    @Test
    void providerTargetsJsonRpcAndOrdinaryJsonRpcConfiguration() {
        JSONRPCCompat03ClientTransportProvider provider = new JSONRPCCompat03ClientTransportProvider();

        assertEquals("JSONRPC", provider.protocolBinding());
        assertEquals("0.3", provider.protocolVersion());
        assertEquals(JSONRPCTransport.class, provider.configuredTransportClass());
        assertTrue(provider.getClass().getPackageName().contains("adapter.jsonrpc"));
    }
}
