package org.a2aproject.sdk.client.http;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Factory for creating {@link A2AHttpClient} instances using the ServiceLoader mechanism.
 *
 * <p>
 * This factory discovers available {@link A2AHttpClientProvider} implementations at runtime,
 * tries them in descending priority order, and returns the first one that succeeds.
 * If no provider can be instantiated, it throws an {@link IllegalStateException}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get the default client (highest available priority provider)
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
 * Providers are tried in descending priority order. If a provider's {@code create()} throws,
 * the factory logs the failure at {@code WARNING} level and falls back to the next provider:
 * <ul>
 * <li>CdiA2AHttpClient: priority 200 (CDI-provided bean, when a container and bean are active)</li>
 * <li>AndroidA2AHttpClient: priority 110 (Android runtime only)</li>
 * <li>VertxA2AHttpClient: priority 100 (when {@code vertx-web-client} is on the classpath)</li>
 * <li>JdkA2AHttpClient: priority 0 (always available, last resort)</li>
 * </ul>
 *
 * <h2>Custom Providers</h2>
 * <p>
 * To add a custom provider, implement {@link A2AHttpClientProvider} and register it
 * in {@code META-INF/services/org.a2aproject.sdk.client.http.A2AHttpClientProvider}.
 */
public final class A2AHttpClientFactory {

    private static final Logger LOGGER = Logger.getLogger(A2AHttpClientFactory.class.getName());
    private static final List<A2AHttpClientProvider> PROVIDERS;

    static {
        PROVIDERS = StreamSupport.stream(ServiceLoader.load(A2AHttpClientProvider.class, A2AHttpClientProvider.class.getClassLoader()).spliterator(), false)
                .sorted(Comparator.comparingInt(A2AHttpClientProvider::priority).reversed())
                .toList();
    }

    private A2AHttpClientFactory() {
        // Utility class
    }

    /**
     * Creates a new A2AHttpClient instance using the highest available priority provider.
     *
     * <p>
     * Providers are tried in descending priority order. If a provider's {@code create()}
     * throws, it is skipped and the next provider is tried. Failures are logged at
     * {@code WARNING} level.
     *
     * @return a new A2AHttpClient instance
     * @throws IllegalStateException if no provider found or all providers failed to instantiate
     */
    public static A2AHttpClient create() {
        return PROVIDERS.stream()
                .flatMap(p -> {
                    try {
                        return Stream.of(p.create());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, e, () -> "Provider " + p.name() + " skipped");
                        return Stream.empty();
                    }
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No A2AHttpClientProvider could be instantiated"));
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

        return PROVIDERS.stream()
                .filter(p -> providerName.equals(p.name()))
                .findFirst()
                .map(A2AHttpClientProvider::create)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No A2AHttpClientProvider found with name: " + providerName));
    }
}
