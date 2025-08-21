package io.a2a.client.transport.grpc;

import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import io.grpc.Channel;

public class GrpcTransportConfigBuilder extends ClientTransportConfigBuilder<GrpcTransportConfig, GrpcTransportConfigBuilder> {

    private Channel channel;

    public GrpcTransportConfigBuilder channel(Channel channel) {
        this.channel = channel;

        return this;
    }

    @Override
    public GrpcTransportConfig build() {
        return new GrpcTransportConfig(channel);
    }
}