package io.a2a.client.transport.rest;

import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

public class RestTransportProvider implements ClientTransportProvider<RestTransport, RestTransportConfig> {

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

    @Override
    public RestTransport create(RestTransportConfig clientTransportConfig, AgentCard agentCard, String agentUrl) throws A2AClientException {
        RestTransportConfig transportConfig = clientTransportConfig;
         if (transportConfig == null) {
            transportConfig = new RestTransportConfig(new JdkA2AHttpClient());
        }
        return new RestTransport(clientTransportConfig.getHttpClient(), agentCard, agentUrl, transportConfig.getInterceptors());
    }

    @Override
    public Class<RestTransport> getTransportProtocolClass() {
        return RestTransport.class;
    }
}
