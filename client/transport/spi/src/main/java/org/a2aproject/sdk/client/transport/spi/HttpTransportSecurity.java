package org.a2aproject.sdk.client.transport.spi;


import org.a2aproject.sdk.client.http.HttpEndpoint;
import org.a2aproject.sdk.spec.AgentInterface;

/** Shared HTTP transport endpoint and redirect policy. */
public final class HttpTransportSecurity {
    private HttpTransportSecurity() {
    }

    /** Validates and canonicalizes a JSON-RPC interface endpoint for transport and credential use.
     * @param endpoint configured interface
     * @return interface with the canonical endpoint
     */
    public static AgentInterface normalizeJsonRpcEndpoint(AgentInterface endpoint) {
        return new AgentInterface(endpoint.protocolBinding(), HttpEndpoint.normalizeJsonRpc(endpoint.url()),
                endpoint.tenant(), endpoint.protocolVersion());
    }
}
