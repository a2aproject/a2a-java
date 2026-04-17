package org.a2aproject.sdk.compat03.client.transport.grpc;

import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder;
import org.a2aproject.sdk.util.Assert;
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