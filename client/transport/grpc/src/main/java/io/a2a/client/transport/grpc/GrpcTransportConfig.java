package io.a2a.client.transport.grpc;

import java.util.function.Function;

import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.util.Assert;
import io.grpc.Channel;

/**
 * Configuration for the gRPC transport protocol.
 * <p>
 * This configuration class allows customization of the gRPC channel factory used for
 * communication with A2A agents. Unlike other transports, gRPC requires a channel factory
 * to be explicitly provided - there is no default implementation.
 * <p>
 * <b>Channel Factory Requirement:</b> You must provide a {@code Function<String, Channel>}
 * that creates gRPC channels from agent URLs. This gives you full control over channel
 * configuration including connection pooling, TLS, load balancing, and interceptors.
 * <p>
 * <b>Basic usage with ManagedChannel:</b>
 * <pre>{@code
 * // Simple insecure channel for development
 * Function<String, Channel> channelFactory = url -> {
 *     String target = extractTarget(url); // e.g., "localhost:9999"
 *     return ManagedChannelBuilder.forTarget(target)
 *         .usePlaintext()
 *         .build();
 * };
 *
 * GrpcTransportConfig config = new GrpcTransportConfigBuilder()
 *     .channelFactory(channelFactory)
 *     .build();
 *
 * Client client = Client.builder(agentCard)
 *     .withTransport(GrpcTransport.class, config)
 *     .build();
 * }</pre>
 * <p>
 * <b>Production configuration with TLS and timeouts:</b>
 * <pre>{@code
 * Function<String, Channel> channelFactory = url -> {
 *     String target = extractTarget(url);
 *     return ManagedChannelBuilder.forTarget(target)
 *         .useTransportSecurity()
 *         .keepAliveTime(30, TimeUnit.SECONDS)
 *         .idleTimeout(5, TimeUnit.MINUTES)
 *         .maxInboundMessageSize(10 * 1024 * 1024) // 10MB
 *         .build();
 * };
 *
 * GrpcTransportConfig config = new GrpcTransportConfigBuilder()
 *     .channelFactory(channelFactory)
 *     .build();
 * }</pre>
 * <p>
 * <b>With load balancing and connection pooling:</b>
 * <pre>{@code
 * Function<String, Channel> channelFactory = url -> {
 *     String target = extractTarget(url);
 *     return ManagedChannelBuilder.forTarget(target)
 *         .defaultLoadBalancingPolicy("round_robin")
 *         .maxInboundMessageSize(50 * 1024 * 1024)
 *         .keepAliveTime(30, TimeUnit.SECONDS)
 *         .keepAliveTimeout(10, TimeUnit.SECONDS)
 *         .build();
 * };
 * }</pre>
 * <p>
 * <b>With interceptors:</b>
 * <pre>{@code
 * GrpcTransportConfig config = new GrpcTransportConfigBuilder()
 *     .channelFactory(channelFactory)
 *     .addInterceptor(new LoggingInterceptor())
 *     .addInterceptor(new AuthInterceptor(apiKey))
 *     .build();
 * }</pre>
 * <p>
 * <b>Channel Lifecycle:</b> The channel factory creates channels on-demand when the client
 * connects to an agent. You are responsible for shutting down channels when the client is
 * closed. Consider using {@code ManagedChannel.shutdown()} in a cleanup hook.
 *
 * @see GrpcTransportConfigBuilder
 * @see GrpcTransport
 * @see io.a2a.client.transport.spi.ClientTransportConfig
 * @see io.grpc.ManagedChannelBuilder
 */
public class GrpcTransportConfig extends ClientTransportConfig<GrpcTransport> {

    private final Function<String, Channel> channelFactory;

    /**
     * Create a gRPC transport configuration with a custom channel factory.
     * <p>
     * Consider using {@link GrpcTransportConfigBuilder} instead for a more fluent API.
     *
     * @param channelFactory function to create gRPC channels from agent URLs (must not be null)
     * @throws IllegalArgumentException if channelFactory is null
     */
    public GrpcTransportConfig(Function<String, Channel> channelFactory) {
        Assert.checkNotNullParam("channelFactory", channelFactory);
        this.channelFactory = channelFactory;
    }

    /**
     * Get the configured channel factory.
     *
     * @return the channel factory function
     */
    public Function<String, Channel> getChannelFactory() {
        return this.channelFactory;
    }
}