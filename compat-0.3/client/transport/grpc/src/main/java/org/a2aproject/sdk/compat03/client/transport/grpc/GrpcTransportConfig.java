package org.a2aproject.sdk.compat03.client.transport.grpc;

import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.util.Assert;
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