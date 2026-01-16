package io.a2a.client.transport.rest;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportConfig;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for the REST transport protocol.
 * <p>
 * This configuration class allows customization of the HTTP client used for RESTful
 * communication with A2A agents. If no HTTP client is specified, the default JDK-based
 * implementation is used.
 * <p>
 * <b>Basic usage:</b>
 * <pre>{@code
 * // Use default HTTP client
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
 * // Custom HTTP client with timeouts
 * A2AHttpClient customClient = new CustomHttpClient()
 *     .withConnectTimeout(Duration.ofSeconds(10))
 *     .withReadTimeout(Duration.ofSeconds(30));
 *
 * RestTransportConfig config = new RestTransportConfigBuilder()
 *     .httpClient(customClient)
 *     .build();
 * }</pre>
 * <p>
 * <b>With interceptors:</b>
 * <pre>{@code
 * RestTransportConfig config = new RestTransportConfigBuilder()
 *     .httpClient(customClient)
 *     .addInterceptor(new LoggingInterceptor())
 *     .addInterceptor(new AuthInterceptor("Bearer token"))
 *     .build();
 * }</pre>
 *
 * @see RestTransportConfigBuilder
 * @see RestTransport
 * @see A2AHttpClient
 * @see io.a2a.client.http.JdkA2AHttpClient
 */
public class RestTransportConfig extends ClientTransportConfig<RestTransport> {

    private final @Nullable A2AHttpClient httpClient;

    /**
     * Create a REST transport configuration with the default HTTP client.
     * <p>
     * The default JDK-based HTTP client will be used. Consider using
     * {@link RestTransportConfigBuilder} instead for a more fluent API.
     */
    public RestTransportConfig() {
        this.httpClient = null;
    }

    /**
     * Create a REST transport configuration with a custom HTTP client.
     * <p>
     * Consider using {@link RestTransportConfigBuilder} instead for a more fluent API.
     *
     * @param httpClient the HTTP client to use for REST requests
     */
    public RestTransportConfig(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Get the configured HTTP client.
     *
     * @return the HTTP client, or {@code null} if using the default
     */
    public @Nullable A2AHttpClient getHttpClient() {
        return httpClient;
    }
}