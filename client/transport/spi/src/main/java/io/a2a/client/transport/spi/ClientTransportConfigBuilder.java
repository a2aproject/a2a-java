package io.a2a.client.transport.spi;

import java.util.ArrayList;
import java.util.List;

import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;

/**
 * Base builder class for constructing transport configuration instances.
 * <p>
 * This abstract builder provides common functionality for building transport configurations,
 * particularly interceptor management. Concrete builders extend this class to add
 * transport-specific configuration options.
 * <p>
 * <b>Self-typed builder pattern:</b> This class uses the "self-typed" or "curiously recurring
 * template pattern" to enable method chaining in subclasses while maintaining type safety:
 * <pre>{@code
 * JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder()
 *     .addInterceptor(loggingInterceptor)  // Returns JSONRPCTransportConfigBuilder
 *     .httpClient(myHttpClient)            // Returns JSONRPCTransportConfigBuilder
 *     .build();                            // Returns JSONRPCTransportConfig
 * }</pre>
 * <p>
 * <b>Interceptor ordering:</b> Interceptors are invoked in the order they were added:
 * <pre>{@code
 * builder
 *     .addInterceptor(authInterceptor)    // Runs first
 *     .addInterceptor(loggingInterceptor) // Runs second
 *     .addInterceptor(metricsInterceptor);// Runs third
 * }</pre>
 *
 * @param <T> the transport configuration type this builder creates
 * @param <B> the concrete builder type (for method chaining)
 * @see ClientTransportConfig
 * @see io.a2a.client.transport.spi.interceptors.ClientCallInterceptor
 */
public abstract class ClientTransportConfigBuilder<T extends ClientTransportConfig<? extends ClientTransport>,
        B extends ClientTransportConfigBuilder<T, B>> {

    protected List<ClientCallInterceptor> interceptors = new ArrayList<>();

    /**
     * Add a request/response interceptor to this transport configuration.
     * <p>
     * Interceptors can be used for cross-cutting concerns such as:
     * <ul>
     *   <li>Logging requests and responses</li>
     *   <li>Adding authentication headers</li>
     *   <li>Collecting metrics and telemetry</li>
     *   <li>Request/response transformation</li>
     *   <li>Error handling and retry logic</li>
     * </ul>
     * <p>
     * Interceptors are invoked in the order they were added. If {@code interceptor} is
     * {@code null}, this method is a no-op (for convenience in conditional addition).
     * <p>
     * Example:
     * <pre>{@code
     * builder
     *     .addInterceptor(new LoggingInterceptor())
     *     .addInterceptor(authToken != null ? new AuthInterceptor(authToken) : null)
     *     .addInterceptor(new MetricsInterceptor());
     * }</pre>
     *
     * @param interceptor the interceptor to add (null values are ignored)
     * @return this builder for method chaining
     * @see io.a2a.client.transport.spi.interceptors.ClientCallInterceptor
     */
    public B addInterceptor(ClientCallInterceptor interceptor) {
        if (interceptor != null) {
            this.interceptors.add(interceptor);
        }

        return (B) this;
    }

    /**
     * Build the transport configuration with all configured options.
     * <p>
     * Concrete implementations should:
     * <ol>
     *   <li>Validate required configuration (e.g., gRPC channel factory)</li>
     *   <li>Apply defaults for optional configuration (e.g., HTTP client)</li>
     *   <li>Create the configuration instance</li>
     *   <li>Transfer interceptors to the configuration</li>
     * </ol>
     *
     * @return the configured transport configuration instance
     * @throws IllegalStateException if required configuration is missing
     */
    public abstract T build();
}
