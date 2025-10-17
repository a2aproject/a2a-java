package io.a2a.client.transport.grpc;

import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import io.a2a.util.Assert;
import io.grpc.Channel;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

public class GrpcTransportConfigBuilder extends ClientTransportConfigBuilder<GrpcTransportConfig, GrpcTransportConfigBuilder> {

    private @Nullable Function<String, Channel> channelFactory;

    public GrpcTransportConfigBuilder channelFactory(Function<String, Channel> channelFactory) {
        Assert.checkNotNullParam("channelFactory", channelFactory);

        this.channelFactory = channelFactory;

        return this;
    }

    @Override
    public GrpcTransportConfig build() {
        if (channelFactory == null) {
            throw new IllegalStateException("channelFactory must be set");
        }
        GrpcTransportConfig config = new GrpcTransportConfig(channelFactory);
        config.setInterceptors(interceptors);
        return config;
    }
}