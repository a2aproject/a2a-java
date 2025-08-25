package io.a2a.client.transport.grpc;

import io.a2a.client.transport.spi.ClientTransportConfig;
import io.grpc.Channel;

public class GrpcTransportConfig extends ClientTransportConfig<GrpcTransport> {

    private final Channel channel;

    public GrpcTransportConfig(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }
}