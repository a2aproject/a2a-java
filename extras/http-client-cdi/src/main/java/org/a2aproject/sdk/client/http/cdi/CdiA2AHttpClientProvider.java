package org.a2aproject.sdk.client.http.cdi;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientProvider;

/**
 * CDI-based {@link A2AHttpClientProvider} that resolves an {@link A2AHttpClient} bean
 * from the CDI container.
 *
 * <p>When a user declares a CDI producer for {@link A2AHttpClient}, this provider
 * picks it up at priority 200 (above Vert.x at 100 and JDK at 0), allowing
 * application-level customization without replacing the ServiceLoader mechanism.
 *
 * <p>If no CDI container is active, or if no unambiguous {@link A2AHttpClient} bean
 * is registered, this provider throws and the factory falls back to the next
 * lower-priority provider.
 *
 * <p><strong>Scope requirement:</strong> The producer must use a normal scope (e.g.
 * {@code @ApplicationScoped}). A {@code @Dependent}-scoped producer will leak a new
 * unmanaged instance on every {@code create()} call, because the {@link Instance}
 * used to obtain the bean is discarded after this method returns and CDI cannot
 * destroy the dependent instance.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Produces
 * @ApplicationScoped
 * public A2AHttpClient myHttpClient(MyConfig config) {
 *     return ...; // custom client
 * }
 * }</pre>
 */
public final class CdiA2AHttpClientProvider implements A2AHttpClientProvider {

    @Override
    public A2AHttpClient create() {
        Instance<A2AHttpClient> instance = CDI.current().select(A2AHttpClient.class);
        if (!instance.isResolvable()) {
            throw new IllegalStateException("No unambiguous A2AHttpClient CDI bean found");
        }
        return instance.get();
    }

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public String name() {
        return "cdi";
    }
}
