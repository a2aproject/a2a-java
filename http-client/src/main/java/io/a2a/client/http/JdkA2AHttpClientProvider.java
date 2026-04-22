package io.a2a.client.http;

/**
 * Service provider for {@link JdkA2AHttpClient}.
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
