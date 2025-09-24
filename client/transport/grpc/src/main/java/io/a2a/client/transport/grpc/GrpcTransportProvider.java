package io.a2a.client.transport.grpc;

import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import io.grpc.Channel;

/**
 * Provider for gRPC transport implementation.
 */
public class GrpcTransportProvider implements ClientTransportProvider<GrpcTransport, GrpcTransportConfig> {

    @Override
    public GrpcTransport create(GrpcTransportConfig grpcTransportConfig, AgentCard agentCard, String agentUrl) throws A2AClientException {
        // not making use of the interceptors for gRPC for now

        Channel channel = grpcTransportConfig.getChannelFactory().apply(agentUrl);
        if (channel != null) {
            return new GrpcTransport(channel, agentCard, grpcTransportConfig.getInterceptors());
        }

        throw new A2AClientException("Missing required GrpcTransportConfig");
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

    @Override
    public Class<GrpcTransport> getTransportProtocolClass() {
        return GrpcTransport.class;
    }
}
