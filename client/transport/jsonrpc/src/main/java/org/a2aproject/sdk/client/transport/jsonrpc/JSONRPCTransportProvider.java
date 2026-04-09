package org.a2aproject.sdk.client.transport.jsonrpc;

import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.transport.spi.ClientTransportProvider;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jspecify.annotations.Nullable;

public class JSONRPCTransportProvider implements ClientTransportProvider<JSONRPCTransport, JSONRPCTransportConfig> {

    @Override
    public JSONRPCTransport create(@Nullable JSONRPCTransportConfig clientTransportConfig, AgentCard agentCard, AgentInterface agentInterface) throws A2AClientException {
        JSONRPCTransportConfig currentClientTransportConfig = clientTransportConfig;
        if (currentClientTransportConfig == null) {
            currentClientTransportConfig = new JSONRPCTransportConfig(A2AHttpClientFactory.create());
        }
        return new JSONRPCTransport(currentClientTransportConfig.getHttpClient(), agentCard, agentInterface, currentClientTransportConfig.getInterceptors());
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
