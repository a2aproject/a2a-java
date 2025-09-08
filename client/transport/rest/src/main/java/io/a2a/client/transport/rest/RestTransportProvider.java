package io.a2a.client.transport.rest;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpClientBuilder;
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
    public RestTransport create(RestTransportConfig transportConfig, AgentCard agentCard, String agentUrl) throws A2AClientException {
        if (transportConfig == null) {
            transportConfig = new RestTransportConfig();
        }

        HttpClientBuilder httpClientBuilder = transportConfig.getHttpClientBuilder();

        try {
            final HttpClient httpClient = httpClientBuilder.create(agentUrl);

            return new RestTransport(httpClient, agentCard, agentUrl, transportConfig.getInterceptors());
        } catch (Exception ex) {
            throw new A2AClientException("Failed to create REST transport", ex);
        }
    }

    @Override
    public Class<RestTransport> getTransportProtocolClass() {
        return RestTransport.class;
    }
}
