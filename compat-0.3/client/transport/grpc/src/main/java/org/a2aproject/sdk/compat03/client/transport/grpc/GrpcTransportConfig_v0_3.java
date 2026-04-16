package org.a2aproject.sdk.compat03.client.transport.grpc;

import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfig_v0_3;
import org.a2aproject.sdk.util.Assert;
import io.grpc.Channel;

import java.util.function.Function;

public class GrpcTransportConfig_v0_3 extends ClientTransportConfig_v0_3<GrpcTransport_v0_3> {

    private final Function<String, Channel> channelFactory;

    public GrpcTransportConfig_v0_3(Function<String, Channel> channelFactory) {
        Assert.checkNotNullParam("channelFactory", channelFactory);
        this.channelFactory = channelFactory;
    }

    public Function<String, Channel> getChannelFactory() {
        return this.channelFactory;
    }
}