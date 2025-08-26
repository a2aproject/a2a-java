package io.a2a.client.transport.grpc;

import java.util.function.Function;

import io.a2a.client.config.ClientTransportConfig;
import io.grpc.Channel;

public class GrpcTransportConfig implements ClientTransportConfig {

    private final Function<String, Channel> channelFactory;

    public GrpcTransportConfig(Function<String, Channel> channelFactory) {
        this.channelFactory = channelFactory;
    }

    public Function<String, Channel> getChannelFactory() {
        return channelFactory;
    }

}