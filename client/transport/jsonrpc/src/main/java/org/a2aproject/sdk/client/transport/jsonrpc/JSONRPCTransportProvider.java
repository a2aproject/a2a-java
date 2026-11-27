package org.a2aproject.sdk.client.transport.jsonrpc;

import org.a2aproject.sdk.client.transport.spi.ClientTransportProvider;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jspecify.annotations.Nullable;

public class JSONRPCTransportProvider implements ClientTransportProvider<JSONRPCTransport, JSONRPCTransportConfig> {

    @Override
    public boolean supportsTenantRouting() {
        return true;
    }

    @Override
    public JSONRPCTransport create(@Nullable JSONRPCTransportConfig clientTransportConfig, AgentCard agentCard, AgentInterface agentInterface) throws A2AClientException {
        JSONRPCTransportConfig config = clientTransportConfig == null ? new JSONRPCTransportConfig() : clientTransportConfig;
        try {
            return new JSONRPCTransport(config.getHttpClient(), agentCard, agentInterface,
                    config.getInterceptors(), config.isFollowRedirects(), config.httpClientConfiguration());
        } catch (IllegalArgumentException e) {
            throw new A2AClientException("Invalid HTTP transport endpoint", e);
        }
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }

    @Override
    public Class<JSONRPCTransport> getTransportProtocolClass() {
        return JSONRPCTransport.class;
    }
}
