package org.a2aproject.sdk.compat03.client.adapter.rest;

import org.a2aproject.sdk.client.VersionedClientTransportProvider;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfig;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportBase;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportSupport;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/** ServiceLoader provider for the optional REST 0.3 adapter. */
public final class RestCompat03ClientTransportProvider implements VersionedClientTransportProvider {
    @Override public String protocolBinding() { return "HTTP+JSON"; }
    @Override public String protocolVersion() { return "0.3"; }
    @Override public Class<? extends ClientTransport> configuredTransportClass() { return RestTransport.class; }

    @Override
    public ClientTransport create(ClientTransportConfig<?> config, AgentCard card, AgentInterface agentInterface)
            throws A2AClientException {
        Compat03ClientTransportSupport.validateAgentInterfaceTenant(agentInterface.tenant());
        RestTransportConfig nativeConfig = config == null ? new RestTransportConfig() :
                (RestTransportConfig) config;
        Compat03ClientTransportSupport.validateConfig(nativeConfig);
        A2AHttpClient httpClient = nativeConfig.getHttpClient();
        RestTransport_v0_3 legacy = new RestTransport_v0_3(httpClient,
                Compat03ClientTransportBase.legacyCard(card), agentInterface.url(), java.util.List.of());
        return new RestCompat03ClientTransport(legacy, card, nativeConfig.getInterceptors());
    }
}
