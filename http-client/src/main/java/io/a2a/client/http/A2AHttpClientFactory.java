package io.a2a.client.http;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

/**
 * Factory for creating {@link A2AHttpClient} instances using the ServiceLoader mechanism.
 */
public final class A2AHttpClientFactory {

    private A2AHttpClientFactory() {
        // Utility class
    }

    /**
     * Creates a new A2AHttpClient instance using the highest priority provider available.
     * If no providers are found, it throws an {@link IllegalStateException}.
     */
    public static A2AHttpClient create() {
        ServiceLoader<A2AHttpClientProvider> loader = ServiceLoader.load(A2AHttpClientProvider.class);

        return StreamSupport.stream(loader.spliterator(), false)
                .max(Comparator.comparingInt(A2AHttpClientProvider::priority))
                .map(A2AHttpClientProvider::create)
                .orElseThrow(() -> new IllegalStateException("No A2AHttpClientProvider found"));
    }

    /**
     * Creates a new A2AHttpClient instance using a specific provider by name.
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
