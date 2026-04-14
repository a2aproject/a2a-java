package io.a2a.client.http;

/**
 * Service provider for {@link AndroidA2AHttpClient}.
 */
public final class AndroidA2AHttpClientProvider implements A2AHttpClientProvider {

    @Override
    public A2AHttpClient create() {
        return new AndroidA2AHttpClient();
    }

    @Override
    public int priority() {
        return 100; // Higher priority than JDK
    }

    @Override
    public String name() {
        return "android";
    }
}
