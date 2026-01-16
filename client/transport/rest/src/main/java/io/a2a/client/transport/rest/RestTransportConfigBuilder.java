package io.a2a.client.transport.rest;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import org.jspecify.annotations.Nullable;

/**
 * Builder for creating {@link RestTransportConfig} instances.
 * <p>
 * This builder provides a fluent API for configuring the REST transport protocol.
 * All configuration options are optional - if not specified, sensible defaults are used:
 * <ul>
 *   <li><b>HTTP client:</b> Auto-selected via {@link A2AHttpClientFactory} (prefers Vert.x, falls back to JDK)</li>
 *   <li><b>Interceptors:</b> None</li>
 * </ul>
 * <p>
 * <b>Basic usage:</b>
 * <pre>{@code
 * // Minimal configuration (uses all defaults)
 * RestTransportConfig config = new RestTransportConfigBuilder()
 *     .build();
 *
 * Client client = Client.builder(agentCard)
 *     .withTransport(RestTransport.class, config)
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
 * RestTransportConfig config = new RestTransportConfigBuilder()
 *     .httpClient(httpClient)
 *     .build();
 * }</pre>
 * <p>
 * <b>With interceptors:</b>
 * <pre>{@code
 * RestTransportConfig config = new RestTransportConfigBuilder()
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
 *     .withTransport(RestTransport.class, new RestTransportConfigBuilder()
 *         .httpClient(customClient)
 *         .addInterceptor(loggingInterceptor))
 *     .build();
 * }</pre>
 *
 * @see RestTransportConfig
 * @see RestTransport
 * @see A2AHttpClient
 * @see io.a2a.client.http.JdkA2AHttpClient
 */
public class RestTransportConfigBuilder extends ClientTransportConfigBuilder<RestTransportConfig, RestTransportConfigBuilder> {

    private @Nullable A2AHttpClient httpClient;

    /**
     * Set the HTTP client to use for REST requests.
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
     * If not specified, a client is auto-selected via {@link A2AHttpClientFactory}.
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
    public RestTransportConfigBuilder httpClient(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Build the REST transport configuration.
     * <p>
     * If no HTTP client was configured, one is auto-selected via {@link A2AHttpClientFactory}.
     * Any configured interceptors are transferred to the configuration.
     *
     * @return the configured REST transport configuration
     */
    @Override
    public RestTransportConfig build() {
        // No HTTP client provided, use factory to get best available implementation
        if (httpClient == null) {
            httpClient = A2AHttpClientFactory.create();
        }

        RestTransportConfig config = new RestTransportConfig(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}