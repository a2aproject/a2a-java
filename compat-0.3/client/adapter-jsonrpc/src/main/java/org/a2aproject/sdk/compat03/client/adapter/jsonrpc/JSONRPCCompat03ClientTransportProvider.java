package org.a2aproject.sdk.compat03.client.adapter.jsonrpc;

import org.a2aproject.sdk.client.VersionedClientTransportProvider;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportBase;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportSupport;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfig_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/** ServiceLoader provider for the optional JSON-RPC 0.3 adapter. */
public final class JSONRPCCompat03ClientTransportProvider implements VersionedClientTransportProvider {
    @Override public String protocolBinding() { return "JSONRPC"; }
    @Override public String protocolVersion() { return "0.3"; }
    @Override public Class<? extends ClientTransport> configuredTransportClass() { return JSONRPCTransport.class; }

    @Override
    public ClientTransport create(ClientTransportConfig<?> config, AgentCard card, AgentInterface agentInterface)
            throws A2AClientException {
        JSONRPCTransportConfig nativeConfig = config == null ? new JSONRPCTransportConfig() :
                (JSONRPCTransportConfig) config;
        Compat03ClientTransportSupport.validateConfig(nativeConfig);
        A2AHttpClient httpClient = nativeConfig.getHttpClient();
        JSONRPCTransport_v0_3 legacy = new JSONRPCTransport_v0_3(httpClient,
                Compat03ClientTransportBase.legacyCard(card), agentInterface.url(), java.util.List.of());
        return new JSONRPCCompat03ClientTransport(legacy, card, nativeConfig.getInterceptors());
    }
}
