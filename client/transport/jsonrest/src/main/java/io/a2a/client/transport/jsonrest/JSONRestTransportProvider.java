package io.a2a.client.transport.jsonrest;

import java.util.List;

import io.a2a.client.config.ClientCallInterceptor;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.config.ClientTransportConfig;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

public class JSONRestTransportProvider implements ClientTransportProvider {

    @Override
    public ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                                  String agentUrl, List<ClientCallInterceptor> interceptors) {
                A2AHttpClient httpClient = null;
        List<ClientTransportConfig> clientTransportConfigs = clientConfig.getClientTransportConfigs();
        if (clientTransportConfigs != null) {
            for (ClientTransportConfig clientTransportConfig : clientTransportConfigs) {
                if (clientTransportConfig instanceof JSONRestTransportConfig jsonrestTransportConfig) {
                    httpClient = jsonrestTransportConfig.getHttpClient();
                    break;
                }
            }
        }
        return new JSONRestTransport(httpClient, agentCard, agentUrl, interceptors);
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }
}
