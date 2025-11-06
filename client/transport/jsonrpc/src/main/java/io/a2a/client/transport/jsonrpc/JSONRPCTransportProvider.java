package io.a2a.client.transport.jsonrpc;

import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import org.jspecify.annotations.Nullable;

public class JSONRPCTransportProvider implements ClientTransportProvider<JSONRPCTransport, JSONRPCTransportConfig> {

    @Override
    public JSONRPCTransport create(@Nullable JSONRPCTransportConfig clientTransportConfig, AgentCard agentCard, String agentUrl) throws A2AClientException {
        JSONRPCTransportConfig currentClientTransportConfig = clientTransportConfig;
        if (currentClientTransportConfig == null) {
            currentClientTransportConfig = new JSONRPCTransportConfig(new JdkA2AHttpClient());
        }
        return new JSONRPCTransport(currentClientTransportConfig.getHttpClient(), agentCard, agentUrl, currentClientTransportConfig.getInterceptors());
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
