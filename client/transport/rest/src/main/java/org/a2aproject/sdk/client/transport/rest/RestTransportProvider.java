package org.a2aproject.sdk.client.transport.rest;

import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.transport.spi.ClientTransportProvider;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;

public class RestTransportProvider implements ClientTransportProvider<RestTransport, RestTransportConfig> {

    @Override
    public boolean supportsTenantRouting() {
        return true;
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

    @Override
    public RestTransport create(RestTransportConfig clientTransportConfig, AgentCard agentCard, AgentInterface agentInterface) throws A2AClientException {
        RestTransportConfig transportConfig = clientTransportConfig == null ? new RestTransportConfig() : clientTransportConfig;
        try {
            return new RestTransport(transportConfig.getHttpClient(), agentCard, agentInterface,
                    transportConfig.getInterceptors(), transportConfig.isFollowRedirects(),
                    () -> A2AHttpClientFactory.createWithOwnership(transportConfig.httpClientConfiguration()));
        } catch (IllegalArgumentException e) {
            throw new A2AClientException("Invalid HTTP transport endpoint", e);
        }
    }

    @Override
    public Class<RestTransport> getTransportProtocolClass() {
        return RestTransport.class;
    }
}
