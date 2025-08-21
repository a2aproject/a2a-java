package io.a2a.client.transport.grpc;

import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

/**
 * Provider for gRPC transport implementation.
 */
public class GrpcTransportProvider implements ClientTransportProvider<GrpcTransport, GrpcTransportConfig> {

    @Override
    public GrpcTransport create(GrpcTransportConfig grpcTransportConfig, AgentCard agentCard, String agentUrl) {
        // not making use of the interceptors for gRPC for now

        Channel channel = grpcTransportConfig.getChannel();

        // no channel factory configured
        if (channel == null) {
            channel = ManagedChannelBuilder.forTarget(agentUrl).build();
        }

        return new GrpcTransport(channel, agentCard);
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
