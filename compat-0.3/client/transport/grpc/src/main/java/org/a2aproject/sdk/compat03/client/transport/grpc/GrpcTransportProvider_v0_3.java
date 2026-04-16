package org.a2aproject.sdk.compat03.client.transport.grpc;

import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportProvider_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import io.grpc.Channel;

/**
 * Provider for gRPC transport implementation.
 */
public class GrpcTransportProvider_v0_3 implements ClientTransportProvider_v0_3<GrpcTransport_v0_3, GrpcTransportConfig_v0_3> {

    @Override
    public GrpcTransport_v0_3 create(GrpcTransportConfig_v0_3 grpcTransportConfig, AgentCard_v0_3 agentCard, String agentUrl) throws A2AClientException_v0_3 {
        // not making use of the interceptors for gRPC for now

        Channel channel = grpcTransportConfig.getChannelFactory().apply(agentUrl);
        if (channel != null) {
            return new GrpcTransport_v0_3(channel, agentCard, grpcTransportConfig.getInterceptors());
        }

        throw new A2AClientException_v0_3("Missing required GrpcTransportConfig");
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol_v0_3.GRPC.asString();
    }

    @Override
    public Class<GrpcTransport_v0_3> getTransportProtocolClass() {
        return GrpcTransport_v0_3.class;
    }
}
