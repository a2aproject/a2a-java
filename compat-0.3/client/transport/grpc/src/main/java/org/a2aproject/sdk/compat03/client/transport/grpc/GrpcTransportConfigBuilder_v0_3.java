package org.a2aproject.sdk.compat03.client.transport.grpc;

import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.util.Assert;
import io.grpc.Channel;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

public class GrpcTransportConfigBuilder_v0_3 extends ClientTransportConfigBuilder_v0_3<GrpcTransportConfig_v0_3, GrpcTransportConfigBuilder_v0_3> {

    private @Nullable Function<String, Channel> channelFactory;

    public GrpcTransportConfigBuilder_v0_3 channelFactory(Function<String, Channel> channelFactory) {
        Assert.checkNotNullParam("channelFactory", channelFactory);

        this.channelFactory = channelFactory;

        return this;
    }

    @Override
    public GrpcTransportConfig_v0_3 build() {
        if (channelFactory == null) {
            throw new IllegalStateException("channelFactory must be set");
        }
        GrpcTransportConfig_v0_3 config = new GrpcTransportConfig_v0_3(channelFactory);
        config.setInterceptors(interceptors);
        return config;
    }
}