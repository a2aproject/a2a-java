package io.a2a.client.transport.grpc;

import java.util.function.Function;

import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import io.a2a.util.Assert;
import io.grpc.Channel;
import org.jspecify.annotations.Nullable;

/**
 * Builder for creating {@link GrpcTransportConfig} instances.
 * <p>
 * This builder provides a fluent API for configuring the gRPC transport protocol.
 * Unlike other transports, gRPC requires a channel factory to be explicitly provided -
 * the {@link #channelFactory(Function)} method must be called before {@link #build()}.
 * <p>
 * The channel factory gives you complete control over gRPC channel configuration:
 * <ul>
 *   <li><b>Connection management:</b> Connection pooling, keep-alive settings</li>
 *   <li><b>Security:</b> TLS configuration, client certificates</li>
 *   <li><b>Performance:</b> Message size limits, compression, load balancing</li>
 *   <li><b>Timeouts:</b> Deadline configuration, idle timeout</li>
 *   <li><b>Interceptors:</b> Request/response transformation, authentication</li>
 * </ul>
 * <p>
 * <b>Basic development setup (insecure):</b>
 * <pre>{@code
 * // Simple channel for local development
 * Function<String, Channel> channelFactory = url -> {
 *     // Extract "localhost:9999" from "http://localhost:9999"
 *     String target = url.replaceAll("^https?://", "");
 *     return ManagedChannelBuilder.forTarget(target)
 *         .usePlaintext()  // No TLS
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
 * <b>Production setup with TLS and connection pooling:</b>
 * <pre>{@code
 * Function<String, Channel> channelFactory = url -> {
 *     String target = extractTarget(url);
 *     return ManagedChannelBuilder.forTarget(target)
 *         .useTransportSecurity()  // Enable TLS
 *         .keepAliveTime(30, TimeUnit.SECONDS)
 *         .keepAliveTimeout(10, TimeUnit.SECONDS)
 *         .idleTimeout(5, TimeUnit.MINUTES)
 *         .maxInboundMessageSize(10 * 1024 * 1024)  // 10MB messages
 *         .build();
 * };
 *
 * GrpcTransportConfig config = new GrpcTransportConfigBuilder()
 *     .channelFactory(channelFactory)
 *     .build();
 * }</pre>
 * <p>
 * <b>With custom SSL certificates:</b>
 * <pre>{@code
 * SslContext sslContext = GrpcSslContexts.forClient()
 *     .trustManager(new File("ca.crt"))
 *     .keyManager(new File("client.crt"), new File("client.key"))
 *     .build();
 *
 * Function<String, Channel> channelFactory = url -> {
 *     String target = extractTarget(url);
 *     return NettyChannelBuilder.forTarget(target)
 *         .sslContext(sslContext)
 *         .build();
 * };
 *
 * GrpcTransportConfig config = new GrpcTransportConfigBuilder()
 *     .channelFactory(channelFactory)
 *     .build();
 * }</pre>
 * <p>
 * <b>With load balancing and health checks:</b>
 * <pre>{@code
 * Function<String, Channel> channelFactory = url -> {
 *     String target = extractTarget(url);
 *     return ManagedChannelBuilder.forTarget(target)
 *         .defaultLoadBalancingPolicy("round_robin")
 *         .enableRetry()
 *         .maxRetryAttempts(3)
 *         .build();
 * };
 *
 * GrpcTransportConfig config = new GrpcTransportConfigBuilder()
 *     .channelFactory(channelFactory)
 *     .build();
 * }</pre>
 * <p>
 * <b>With A2A interceptors:</b>
 * <pre>{@code
 * GrpcTransportConfig config = new GrpcTransportConfigBuilder()
 *     .channelFactory(channelFactory)
 *     .addInterceptor(new LoggingInterceptor())
 *     .addInterceptor(new MetricsInterceptor())
 *     .addInterceptor(new AuthenticationInterceptor(apiKey))
 *     .build();
 * }</pre>
 * <p>
 * <b>Direct usage in ClientBuilder:</b>
 * <pre>{@code
 * // Channel factory inline
 * Client client = Client.builder(agentCard)
 *     .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder()
 *         .channelFactory(url -> ManagedChannelBuilder
 *             .forTarget(extractTarget(url))
 *             .usePlaintext()
 *             .build())
 *         .addInterceptor(loggingInterceptor))
 *     .build();
 * }</pre>
 * <p>
 * <b>Channel Lifecycle Management:</b>
 * <pre>{@code
 * // Store channels for cleanup
 * Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
 *
 * Function<String, Channel> channelFactory = url -> {
 *     return channels.computeIfAbsent(url, u -> {
 *         String target = extractTarget(u);
 *         return ManagedChannelBuilder.forTarget(target)
 *             .usePlaintext()
 *             .build();
 *     });
 * };
 *
 * // Cleanup when done
 * Runtime.getRuntime().addShutdownHook(new Thread(() -> {
 *     channels.values().forEach(ManagedChannel::shutdown);
 * }));
 * }</pre>
 *
 * @see GrpcTransportConfig
 * @see GrpcTransport
 * @see io.a2a.client.transport.spi.ClientTransportConfigBuilder
 * @see io.grpc.ManagedChannelBuilder
 * @see io.grpc.Channel
 */
public class GrpcTransportConfigBuilder extends ClientTransportConfigBuilder<GrpcTransportConfig, GrpcTransportConfigBuilder> {

    private @Nullable Function<String, Channel> channelFactory;

    /**
     * Set the channel factory for creating gRPC channels.
     * <p>
     * <b>This method is required</b> - {@link #build()} will throw {@link IllegalStateException}
     * if the channel factory is not set.
     * <p>
     * The factory function receives the agent's URL (e.g., "http://localhost:9999") and must
     * return a configured {@link Channel}. You are responsible for:
     * <ul>
     *   <li>Extracting the target address from the URL</li>
     *   <li>Configuring TLS and security settings</li>
     *   <li>Setting connection pool and timeout parameters</li>
     *   <li>Managing channel lifecycle and shutdown</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * Function<String, Channel> factory = url -> {
     *     String target = url.replaceAll("^https?://", "");
     *     return ManagedChannelBuilder.forTarget(target)
     *         .usePlaintext()
     *         .build();
     * };
     *
     * builder.channelFactory(factory);
     * }</pre>
     *
     * @param channelFactory function to create gRPC channels from agent URLs (must not be null)
     * @return this builder for method chaining
     * @throws IllegalArgumentException if channelFactory is null
     */
    public GrpcTransportConfigBuilder channelFactory(Function<String, Channel> channelFactory) {
        Assert.checkNotNullParam("channelFactory", channelFactory);

        this.channelFactory = channelFactory;

        return this;
    }

    /**
     * Build the gRPC transport configuration.
     * <p>
     * The channel factory must have been set via {@link #channelFactory(Function)} before
     * calling this method. Any configured interceptors are transferred to the configuration.
     *
     * @return the configured gRPC transport configuration
     * @throws IllegalStateException if the channel factory was not set
     */
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