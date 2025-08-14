package io.a2a.client.transport;

import java.util.List;

import io.a2a.client.ClientCallInterceptor;
import io.a2a.client.ClientConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

public class JSONRestTransportProvider implements ClientTransportProvider {

    @Override
    public ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                                  String agentUrl, List<ClientCallInterceptor> interceptors) {
        return new JSONRestTransport(clientConfig.getHttpClient(), agentCard, agentUrl, interceptors);
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }
}
