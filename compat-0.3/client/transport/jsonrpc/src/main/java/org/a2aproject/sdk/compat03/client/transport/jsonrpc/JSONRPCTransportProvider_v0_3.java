package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportProvider_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

public class JSONRPCTransportProvider_v0_3 implements ClientTransportProvider_v0_3<JSONRPCTransport_v0_3, JSONRPCTransportConfig_v0_3> {

    @Override
    public JSONRPCTransport_v0_3 create(JSONRPCTransportConfig_v0_3 clientTransportConfig, AgentCard_v0_3 agentCard, String agentUrl) throws A2AClientException_v0_3 {
        if (clientTransportConfig == null) {
            clientTransportConfig = new JSONRPCTransportConfig_v0_3(new JdkA2AHttpClient_v0_3());
        }

        return new JSONRPCTransport_v0_3(clientTransportConfig.getHttpClient(), agentCard, agentUrl, clientTransportConfig.getInterceptors());
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol_v0_3.JSONRPC.asString();
    }

    @Override
    public Class<JSONRPCTransport_v0_3> getTransportProtocolClass() {
        return JSONRPCTransport_v0_3.class;
    }
}
