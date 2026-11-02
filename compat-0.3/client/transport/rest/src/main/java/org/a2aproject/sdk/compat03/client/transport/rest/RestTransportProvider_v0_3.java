package org.a2aproject.sdk.compat03.client.transport.rest;

import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportProvider_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

public class RestTransportProvider_v0_3
        implements ClientTransportProvider_v0_3<RestTransport_v0_3, RestTransportConfig_v0_3> {

    @Override
    public String getTransportProtocol() {
        return TransportProtocol_v0_3.HTTP_JSON.asString();
    }

    @Override
    public RestTransport_v0_3 create(
            RestTransportConfig_v0_3 clientTransportConfig,
            AgentCard_v0_3 agentCard,
            String agentUrl)
            throws A2AClientException_v0_3 {
        if (clientTransportConfig == null) {
            clientTransportConfig = new RestTransportConfig_v0_3(A2AHttpClientFactory.create());
        }
        return new RestTransport_v0_3(
                clientTransportConfig.getHttpClient(),
                agentCard,
                agentUrl,
                clientTransportConfig.getInterceptors());
    }

    @Override
    public Class<RestTransport_v0_3> getTransportProtocolClass() {
        return RestTransport_v0_3.class;
    }
}
