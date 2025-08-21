package io.a2a.client.transport.grpc;

import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.util.Assert;
import io.grpc.Channel;

import java.util.function.Function;

public class GrpcTransportConfig extends ClientTransportConfig<GrpcTransport> {

    private final Function<String, Channel> channelFactory;

    public GrpcTransportConfig(Function<String, Channel> channelFactory) {
        Assert.checkNotNullParam("channelFactory", channelFactory);
        this.channelFactory = channelFactory;
    }

    public Function<String, Channel> getChannelFactory() {
        return this.channelFactory;
    }
}