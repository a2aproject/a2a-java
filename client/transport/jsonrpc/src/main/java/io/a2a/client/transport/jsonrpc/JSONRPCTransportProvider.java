package io.a2a.client.transport.jsonrpc;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

public class JSONRPCTransportProvider implements ClientTransportProvider<JSONRPCTransport, JSONRPCTransportConfig> {

    @Override
    public JSONRPCTransport create(JSONRPCTransportConfig transportConfig, AgentCard agentCard, String agentUrl) throws A2AClientException {
        if (transportConfig == null) {
            transportConfig = new JSONRPCTransportConfig();
        }

        HttpClientBuilder httpClientBuilder = transportConfig.getHttpClientBuilder();

        try {
            final HttpClient httpClient = httpClientBuilder.create(agentUrl);

            return new JSONRPCTransport(httpClient, agentCard, agentUrl, transportConfig.getInterceptors());
        } catch (Exception ex) {
            throw new A2AClientException("Failed to create JSONRPC transport", ex);
        }
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
