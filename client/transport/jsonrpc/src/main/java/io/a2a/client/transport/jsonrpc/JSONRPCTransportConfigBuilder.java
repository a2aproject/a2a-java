package io.a2a.client.transport.jsonrpc;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Builder for creating {@link JSONRPCTransportConfig} instances.
 * <p>
 * This builder provides a fluent API for configuring the JSON-RPC transport protocol.
 * All configuration options are optional - if not specified, sensible defaults are used:
 * <ul>
 *   <li><b>HTTP client:</b> {@link JdkA2AHttpClient} (JDK's built-in HTTP client)</li>
 *   <li><b>Interceptors:</b> None</li>
 * </ul>
 * <p>
 * <b>Basic usage:</b>
 * <pre>{@code
 * // Minimal configuration (uses all defaults)
 * JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder()
 *     .build();
 *
 * Client client = Client.builder(agentCard)
 *     .withTransport(JSONRPCTransport.class, config)
 *     .build();
 * }</pre>
 * <p>
 * <b>Custom HTTP client:</b>
 * <pre>{@code
 * // Configure custom HTTP client for connection pooling, timeouts, etc.
 * A2AHttpClient httpClient = new ApacheHttpClient()
 *     .withConnectionTimeout(Duration.ofSeconds(10))
 *     .withMaxConnections(50);
 *
 * JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder()
 *     .httpClient(httpClient)
 *     .build();
 * }</pre>
 * <p>
 * <b>With interceptors:</b>
 * <pre>{@code
 * JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder()
 *     .addInterceptor(new LoggingInterceptor())
 *     .addInterceptor(new MetricsInterceptor())
 *     .addInterceptor(new RetryInterceptor(3))
 *     .build();
 * }</pre>
 * <p>
 * <b>Direct usage in ClientBuilder:</b>
 * <pre>{@code
 * // Can pass builder directly to withTransport()
 * Client client = Client.builder(agentCard)
 *     .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
 *         .httpClient(customClient)
 *         .addInterceptor(loggingInterceptor))
 *     .build();
 * }</pre>
 *
 * @see JSONRPCTransportConfig
 * @see JSONRPCTransport
 * @see A2AHttpClient
 * @see io.a2a.client.http.JdkA2AHttpClient
 */
public class JSONRPCTransportConfigBuilder extends ClientTransportConfigBuilder<JSONRPCTransportConfig, JSONRPCTransportConfigBuilder> {

    private @Nullable A2AHttpClient httpClient;

    /**
     * Set the HTTP client to use for JSON-RPC requests.
     * <p>
     * Custom HTTP clients can provide:
     * <ul>
     *   <li>Connection pooling and reuse</li>
     *   <li>Custom timeout configuration</li>
     *   <li>SSL/TLS configuration</li>
     *   <li>Proxy support</li>
     *   <li>Custom header handling</li>
     * </ul>
     * <p>
     * If not specified, the default {@link JdkA2AHttpClient} is used.
     * <p>
     * Example:
     * <pre>{@code
     * A2AHttpClient client = new CustomHttpClient()
     *     .withConnectTimeout(Duration.ofSeconds(5))
     *     .withReadTimeout(Duration.ofSeconds(30))
     *     .withConnectionPool(10, 50);
     *
     * builder.httpClient(client);
     * }</pre>
     *
     * @param httpClient the HTTP client to use
     * @return this builder for method chaining
     */
    public JSONRPCTransportConfigBuilder httpClient(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Build the JSON-RPC transport configuration.
     * <p>
     * If no HTTP client was configured, the default {@link JdkA2AHttpClient} is used.
     * Any configured interceptors are transferred to the configuration.
     *
     * @return the configured JSON-RPC transport configuration
     */
    @Override
    public JSONRPCTransportConfig build() {
        // No HTTP client provided, fallback to the default one (JDK-based implementation)
        if (httpClient == null) {
            httpClient = new JdkA2AHttpClient();
        }

        JSONRPCTransportConfig config = new JSONRPCTransportConfig(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}
