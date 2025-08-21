package io.a2a.client.transport.jsonrpc;

import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

public class JSONRPCTransportProvider implements ClientTransportProvider<JSONRPCTransport, JSONRPCTransportConfig> {

    @Override
    public JSONRPCTransport create(JSONRPCTransportConfig clientTransportConfig, AgentCard agentCard, String agentUrl) throws A2AClientException {
        if (clientTransportConfig == null) {
            clientTransportConfig = new JSONRPCTransportConfig(new JdkA2AHttpClient());
        }

        return new JSONRPCTransport(clientTransportConfig.getHttpClient(), agentCard, agentUrl, clientTransportConfig.getInterceptors());
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
