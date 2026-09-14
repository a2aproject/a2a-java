package org.a2aproject.sdk.compat03.client.adapter.grpc;

import java.util.Objects;

import org.a2aproject.sdk.client.VersionedClientTransportProvider;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfig;
import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportBase;
import org.a2aproject.sdk.compat03.client.adapter.Compat03ClientTransportSupport;
import org.a2aproject.sdk.compat03.client.transport.grpc.GrpcTransport_v0_3;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/** ServiceLoader provider for the optional gRPC 0.3 adapter. */
public final class GrpcCompat03ClientTransportProvider implements VersionedClientTransportProvider {
    @Override
    public String protocolBinding() {
        return "GRPC";
    }

    @Override
    public String protocolVersion() {
        return "0.3";
    }

    @Override
    public Class<? extends ClientTransport> configuredTransportClass() {
        return GrpcTransport.class;
    }

    @Override
    public ClientTransport create(ClientTransportConfig<?> config, AgentCard card, AgentInterface agentInterface)
            throws A2AClientException {
        if (!(config instanceof GrpcTransportConfig grpcConfig)) {
            throw new A2AClientException("Expected GrpcTransportConfig for the gRPC 0.3 adapter");
        }
        Compat03ClientTransportSupport.validateConfig(grpcConfig);
        GrpcTransport_v0_3 legacy = new GrpcTransport_v0_3(
                Objects.requireNonNull(grpcConfig.getChannelFactory().apply(agentInterface.url()),
                        "channelFactory returned null"),
                Compat03ClientTransportBase.legacyCard(card));
        return new GrpcCompat03ClientTransport(legacy, card, grpcConfig.getInterceptors());
    }
}
