package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportProvider;
import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.TransportProtocol;

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
