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
