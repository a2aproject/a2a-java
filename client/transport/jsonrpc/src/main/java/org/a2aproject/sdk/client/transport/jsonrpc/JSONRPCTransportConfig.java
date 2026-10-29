package org.a2aproject.sdk.client.transport.jsonrpc;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.jspecify.annotations.Nullable;

/**
 * Configuration for the JSON-RPC transport protocol.
 * <p>
 * This configuration class allows customization of the HTTP client used for JSON-RPC
 * communication with A2A agents. If no HTTP client is specified, the default JDK-based
 * implementation is used.
 * <p>
 * <b>Basic usage:</b>
 * <pre>{@code
 * // Use default HTTP client
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
 * // Custom HTTP client with timeouts
 * A2AHttpClient customClient = new CustomHttpClient()
 *     .withConnectTimeout(Duration.ofSeconds(10))
 *     .withReadTimeout(Duration.ofSeconds(30));
 *
 * JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder()
 *     .httpClient(customClient)
 *     .build();
 * }</pre>
 * <p>
 * <b>With interceptors:</b>
 * <pre>{@code
 * JSONRPCTransportConfig config = new JSONRPCTransportConfigBuilder()
 *     .httpClient(customClient)
 *     .addInterceptor(new LoggingInterceptor())
 *     .addInterceptor(new AuthInterceptor("Bearer token"))
 *     .build();
 * }</pre>
 *
 * @see JSONRPCTransportConfigBuilder
 * @see JSONRPCTransport
 * @see A2AHttpClient
 * @see org.a2aproject.sdk.client.http.JdkA2AHttpClient
 */
public class JSONRPCTransportConfig extends ClientTransportConfig<JSONRPCTransport> {

    private final @Nullable A2AHttpClient httpClient;
    private final boolean followRedirects;

    /**
     * Create a JSON-RPC transport configuration with the default HTTP client.
     * <p>
     * The default JDK-based HTTP client will be used. Consider using
     * {@link JSONRPCTransportConfigBuilder} instead for a more fluent API.
     */
    public JSONRPCTransportConfig() {
        this(null, false);
    }

    /**
     * Create a JSON-RPC transport configuration with a custom HTTP client.
     * <p>
     * Consider using {@link JSONRPCTransportConfigBuilder} instead for a more fluent API.
     *
     * @param httpClient the HTTP client to use for JSON-RPC requests
     */
    public JSONRPCTransportConfig(A2AHttpClient httpClient) {
        this(httpClient, false);
    }

    /**
     * Create a JSON-RPC transport configuration with a custom HTTP client and redirect policy.
     *
     * @param httpClient the HTTP client to use for JSON-RPC requests
     * @param followRedirects whether POST requests may follow a single redirect
     */
    public JSONRPCTransportConfig(@Nullable A2AHttpClient httpClient, boolean followRedirects) {
        this.httpClient = httpClient;
        this.followRedirects = followRedirects;
    }

    /**
     * Get the configured HTTP client.
     *
     * @return the HTTP client, or {@code null} if using the default
     */
    public @Nullable A2AHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Whether POST requests issued by this transport may follow a redirect.
     *
     * @return {@code true} when redirect following is enabled
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }
}
