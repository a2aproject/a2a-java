package org.a2aproject.sdk.client.transport.grpc;

import org.a2aproject.sdk.client.transport.spi.ClientTransportProvider;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import io.grpc.Channel;

/**
 * Provider for gRPC transport implementation.
 */
public class GrpcTransportProvider implements ClientTransportProvider<GrpcTransport, GrpcTransportConfig> {

    @Override
    public GrpcTransport create(GrpcTransportConfig grpcTransportConfig, AgentCard agentCard, AgentInterface agentInterface) throws A2AClientException {
        // not making use of the interceptors for gRPC for now

        Channel channel = grpcTransportConfig.getChannelFactory().apply(agentInterface.url());
        if (channel != null) {
            return new GrpcTransport(channel, agentCard, agentInterface.tenant(), grpcTransportConfig.getInterceptors());
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
