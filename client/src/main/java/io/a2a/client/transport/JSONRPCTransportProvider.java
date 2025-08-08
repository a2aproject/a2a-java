package io.a2a.client.transport;

import java.util.List;

import io.a2a.client.ClientCallInterceptor;
import io.a2a.client.ClientConfig;
import io.a2a.http.A2AHttpClient;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

public class JSONRPCTransportProvider implements ClientTransportProvider {

    @Override
    public ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                                  String agentUrl, List<ClientCallInterceptor> interceptors) {
        return new JSONRPCTransport(clientConfig.getHttpClient(), agentCard, agentUrl, interceptors);
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }
}
