package io.a2a.client.http;

/**
 * Service provider interface for creating {@link A2AHttpClient} instances.
 *
 * <p>
 * Implementations of this interface can be registered via the Java ServiceLoader
 * mechanism. The {@link A2AHttpClientFactory} will discover and use the highest
 * priority provider available.
 *
 * <p>
 * To register a provider, create a file named
 * {@code META-INF/services/io.a2a.client.http.A2AHttpClientProvider} containing
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
     * Returns the priority of this provider. Higher priority providers are
     * preferred over lower priority ones.
     *
     * <p>
     * Default priorities:
     * <ul>
     * <li>JdkA2AHttpClient: 0 (fallback)</li>
     * <li>VertxA2AHttpClient: 100 (preferred when available)</li>
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
