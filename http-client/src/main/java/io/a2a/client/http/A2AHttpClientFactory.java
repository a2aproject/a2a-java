package io.a2a.client.http;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

/**
 * Factory for creating {@link A2AHttpClient} instances using the ServiceLoader mechanism.
 *
 * <p>
 * This factory discovers available {@link A2AHttpClientProvider} implementations at runtime
 * and selects the one with the highest priority. If no providers are found, it falls back
 * to creating a {@link JdkA2AHttpClient}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get the default client (highest priority provider)
 * A2AHttpClient client = A2AHttpClientFactory.create();
 *
 * // Use with try-with-resources if the client implements AutoCloseable
 * try (A2AHttpClient client = A2AHttpClientFactory.create()) {
 *     A2AHttpResponse response = client.createGet()
 *         .url("https://example.com")
 *         .get();
 * }
 * }</pre>
 *
 * <h2>Priority System</h2>
 * <p>
 * Providers are selected based on their priority value (higher is better):
 * <ul>
 * <li>JdkA2AHttpClient: priority 0 (fallback)</li>
 * <li>VertxA2AHttpClient: priority 100 (preferred when available)</li>
 * </ul>
 *
 * <h2>Custom Providers</h2>
 * <p>
 * To add a custom provider, implement {@link A2AHttpClientProvider} and register it
 * in {@code META-INF/services/io.a2a.client.http.A2AHttpClientProvider}.
 */
public final class A2AHttpClientFactory {

    private A2AHttpClientFactory() {
        // Utility class
    }

    /**
     * Creates a new A2AHttpClient instance using the highest priority provider available.
     *
     * <p>
     * This method uses the ServiceLoader mechanism to discover providers at runtime.
     * If no providers are found, it falls back to creating a {@link JdkA2AHttpClient}.
     *
     * @return a new A2AHttpClient instance
     */
    public static A2AHttpClient create() {
        ServiceLoader<A2AHttpClientProvider> loader = ServiceLoader.load(A2AHttpClientProvider.class);

        return StreamSupport.stream(loader.spliterator(), false)
                .max(Comparator.comparingInt(A2AHttpClientProvider::priority))
                .map(A2AHttpClientProvider::create)
                .orElseGet(JdkA2AHttpClient::new);
    }

    /**
     * Creates a new A2AHttpClient instance using a specific provider by name.
     *
     * <p>
     * This method is useful for testing or when you need to force a specific implementation.
     *
     * @param providerName the name of the provider to use
     * @return a new A2AHttpClient instance from the specified provider
     * @throws IllegalArgumentException if no provider with the given name is found
     */
    public static A2AHttpClient create(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            throw new IllegalArgumentException("Provider name must not be null or empty");
        }

        ServiceLoader<A2AHttpClientProvider> loader = ServiceLoader.load(A2AHttpClientProvider.class);

        return StreamSupport.stream(loader.spliterator(), false)
                .filter(provider -> providerName.equals(provider.name()))
                .findFirst()
                .map(A2AHttpClientProvider::create)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No A2AHttpClientProvider found with name: " + providerName));
    }
}
