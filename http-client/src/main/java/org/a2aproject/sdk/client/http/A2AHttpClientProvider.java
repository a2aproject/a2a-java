package org.a2aproject.sdk.client.http;

/**
 * Service provider interface for creating {@link A2AHttpClient} instances.
 *
 * <p>
 * Implementations of this interface can be registered via the Java ServiceLoader
 * mechanism. The {@link A2AHttpClientFactory} discovers all registered providers,
 * sorts them by descending {@link #priority()}, and tries each in order, returning
 * the first one whose {@link #create()} succeeds.
 *
 * <p>
 * To register a provider, create a file named
 * {@code META-INF/services/org.a2aproject.sdk.client.http.A2AHttpClientProvider} containing
 * the fully qualified class name of your provider implementation.
 */
public interface A2AHttpClientProvider {

    /**
     * Creates a new instance of an A2AHttpClient.
     *
     * @return a new A2AHttpClient instance
     */
    A2AHttpClient create();

    /**
     * Creates a new instance of an A2AHttpClient with the given {@link SSEParserConfig}.
     *
     * <p>Providers that support SSE parser configuration should override both this method
     * and {@link #supportsSseConfig()} to return {@code true}.
     * The default implementation ignores {@code sseParserConfig} and delegates to {@link #create()}.
     *
     * @param sseParserConfig the SSE parser configuration to apply
     * @return a new A2AHttpClient instance
     */
    default A2AHttpClient createWithSseConfig(SSEParserConfig sseParserConfig) {
        return create();
    }

    /**
     * Returns {@code true} if this provider honours the {@link SSEParserConfig} passed to
     * {@link #createWithSseConfig(SSEParserConfig)}.
     *
     * <p>Providers that override {@link #createWithSseConfig} to actually apply the
     * configuration must also override this method and return {@code true}; the
     * {@link A2AHttpClientFactory#createWithSseConfig} method uses this flag to skip
     * providers that would silently ignore the supplied configuration.
     *
     * <p>The default is {@code false}, matching the default no-op implementation of
     * {@link #createWithSseConfig}.
     *
     * @return {@code true} if this provider applies the given {@link SSEParserConfig}
     */
    default boolean supportsSseConfig() {
        return false;
    }

    /**
     * Returns the priority of this provider. Higher priority providers are
     * tried first; the first one whose {@link #create()} succeeds is used.
     *
     * <p>
     * Built-in priorities (for reference when choosing a custom value):
     * <ul>
     * <li>CdiA2AHttpClient: 200 (CDI-provided bean, when a container and bean are active)</li>
     * <li>AndroidA2AHttpClient: 110 (Android runtime only)</li>
     * <li>VertxA2AHttpClient: 100 (when {@code vertx-web-client} is on the classpath)</li>
     * <li>JdkA2AHttpClient: 0 (always available, last resort)</li>
     * </ul>
     *
     * @return the priority value (higher is better)
     */
    default int priority() {
        return 0;
    }

    /**
     * Returns the name of this provider for logging and debugging purposes.
     *
     * @return the provider name
     */
    String name();
}
