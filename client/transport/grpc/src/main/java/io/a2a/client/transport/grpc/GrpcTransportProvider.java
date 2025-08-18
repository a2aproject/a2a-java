package io.a2a.client.transport.grpc;

import java.util.List;

import io.a2a.client.config.ClientCallInterceptor;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

/**
 * Provider for gRPC transport implementation.
 */
public class GrpcTransportProvider implements ClientTransportProvider {

    @Override
    public ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                                  String agentUrl, List<ClientCallInterceptor> interceptors) {
        // not making use of the interceptors for gRPC for now
        return new GrpcTransport(clientConfig.getChannel(), agentCard);
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

}
