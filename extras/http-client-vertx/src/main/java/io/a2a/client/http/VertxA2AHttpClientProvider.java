package io.a2a.client.http;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service provider for {@link VertxA2AHttpClient}.
 *
 * <p>
 * This provider has a higher priority (100) than the JDK implementation and will be
 * preferred when the Vert.x dependencies are available on the classpath.
 *
 * <p>
 * If Vert.x classes are not available at runtime, this provider will check for their
 * presence and throw an {@link IllegalStateException} when attempting to create a client.
 * The ServiceLoader mechanism will skip this provider and fall back to the JDK implementation.
 */
public final class VertxA2AHttpClientProvider implements A2AHttpClientProvider {

    private static final boolean VERTX_AVAILABLE = isVertxAvailable();
    private static final Logger log = Logger.getLogger(VertxA2AHttpClientProvider.class.getName());

    private static boolean isVertxAvailable() {
        try {
            Class.forName("io.vertx.core.Vertx");
            Class.forName("io.vertx.ext.web.client.WebClient");
            return true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(VertxA2AHttpClientProvider.class.getName()).log(Level.FINE, "Vert.x classes are not available on the classpath. Falling back to other providers.", ex);
            return false;
        }
    }

    @Override
    public A2AHttpClient create() {
        if (!VERTX_AVAILABLE) {
            throw new IllegalStateException(
                    "Vert.x classes are not available on the classpath. "
                    + "Add io.vertx:vertx-web-client dependency or use the JDK HTTP client implementation.");
        }

        try {
            Class<?> clientClass = Class.forName("io.a2a.client.http.VertxA2AHttpClient");
            return (A2AHttpClient) clientClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create VertxA2AHttpClient instance", e);
        }
    }

    @Override
    public int priority() {
        return VERTX_AVAILABLE ? 100 : -1; // Higher priority when available, negative when not
    }

    @Override
    public String name() {
        return "vertx";
    }
}
