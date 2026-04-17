package org.a2aproject.sdk.compat03.client.transport.rest;

import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportProvider;
import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.TransportProtocol;

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
