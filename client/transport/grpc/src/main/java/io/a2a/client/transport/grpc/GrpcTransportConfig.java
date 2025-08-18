package io.a2a.client.transport.grpc;

import io.a2a.client.config.ClientTransportConfig;
import io.grpc.ManagedChannelBuilder;

public class GrpcTransportConfig implements ClientTransportConfig {

    private final ManagedChannelBuilder<?> channelBuilder;

    public GrpcTransportConfig(ManagedChannelBuilder<?> channelBuilder) {
        this.channelBuilder = channelBuilder;
    }

    public ManagedChannelBuilder<?> getManagedChannelBuilder() {
        return channelBuilder;
    }
}