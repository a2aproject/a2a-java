package io.a2a.client.http;

/**
 * Service provider for {@link JdkA2AHttpClient}.
 *
 * <p>
 * This provider has the lowest priority (0) and serves as the fallback implementation
 * when no other providers are available. The JDK HTTP client is always available as it
 * uses only standard Java libraries.
 */
public final class JdkA2AHttpClientProvider implements A2AHttpClientProvider {

    @Override
    public A2AHttpClient create() {
        return new JdkA2AHttpClient();
    }

    @Override
    public int priority() {
        return 0; // Lowest priority - fallback
    }

    @Override
    public String name() {
        return "jdk";
    }
}
