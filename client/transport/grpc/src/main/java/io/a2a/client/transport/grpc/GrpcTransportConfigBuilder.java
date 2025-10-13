package io.a2a.client.transport.grpc;

import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import io.a2a.util.Assert;
import io.grpc.Channel;

import java.util.function.Function;

public class GrpcTransportConfigBuilder extends ClientTransportConfigBuilder<GrpcTransportConfig, GrpcTransportConfigBuilder> {

    private Function<String, Channel> channelFactory = s -> { throw new IllegalStateException("channelFactory not set"); };

    public GrpcTransportConfigBuilder channelFactory(Function<String, Channel> channelFactory) {
        Assert.checkNotNullParam("channelFactory", channelFactory);

        this.channelFactory = channelFactory;

        return this;
    }

    @Override
    public GrpcTransportConfig build() {
        GrpcTransportConfig config = new GrpcTransportConfig(channelFactory);
        config.setInterceptors(interceptors);
        return config;
    }
}