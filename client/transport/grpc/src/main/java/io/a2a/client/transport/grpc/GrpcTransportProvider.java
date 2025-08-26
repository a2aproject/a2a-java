package io.a2a.client.transport.grpc;

import java.util.List;

import io.a2a.client.config.ClientCallInterceptor;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.config.ClientTransportConfig;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import io.grpc.Channel;

/**
 * Provider for gRPC transport implementation.
 */
public class GrpcTransportProvider implements ClientTransportProvider {

    @Override
    public ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                                  String agentUrl, List<ClientCallInterceptor> interceptors) throws A2AClientException {
        // not making use of the interceptors for gRPC for now
        List<ClientTransportConfig> clientTransportConfigs = clientConfig.getClientTransportConfigs();
        if (clientTransportConfigs != null) {
            for (ClientTransportConfig clientTransportConfig : clientTransportConfigs) {
                if (clientTransportConfig instanceof GrpcTransportConfig grpcTransportConfig) {
                    Channel channel = grpcTransportConfig.getChannelFactory().apply(agentUrl);
                    return new GrpcTransport(channel, agentCard);
                }
            }
        }
        throw new A2AClientException("Missing required GrpcTransportConfig");
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

}
