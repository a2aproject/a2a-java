package io.a2a.client.http;

/**
 * Provider interface for creating {@link A2AHttpClient} instances.
 */
public interface A2AHttpClientProvider {
    /**
     * Creates a new A2AHttpClient instance.
     */
    A2AHttpClient create();

    /**
     * Returns the priority of this provider. Higher priority providers are preferred.
     */
    default int priority() {
        return 0;
    }

    /**
     * Returns the name of this provider.
     */
    String name();
}
