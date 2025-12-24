package io.a2a.client.transport.spi;

import java.util.ArrayList;
import java.util.List;

import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;

/**
 * Base configuration class for A2A client transport protocols.
 * <p>
 * This abstract class provides common configuration functionality for all transport implementations
 * (JSON-RPC, gRPC, REST). It manages request/response interceptors that can be used for logging,
 * metrics, authentication, and other cross-cutting concerns.
 * <p>
 * <b>Interceptors:</b> Transport configurations support adding interceptors that can inspect and
 * modify requests/responses. Interceptors are invoked in the order they were added:
 * <pre>{@code
 * // Example: Add logging and authentication interceptors
 * config.setInterceptors(List.of(
 *     new LoggingInterceptor(),
 *     new AuthenticationInterceptor("Bearer token")
 * ));
 * }</pre>
 * <p>
 * Concrete implementations typically extend this class to add transport-specific configuration
 * such as HTTP clients, gRPC channels, or connection pools.
 * <p>
 * <b>Thread safety:</b> Configuration instances should be treated as immutable after construction.
 * The interceptor list is copied defensively to prevent external modification.
 *
 * @param <T> the transport type this configuration is for
 * @see ClientTransportConfigBuilder
 * @see io.a2a.client.transport.spi.interceptors.ClientCallInterceptor
 */
public abstract class ClientTransportConfig<T extends ClientTransport> {

    protected List<ClientCallInterceptor> interceptors = new ArrayList<>();

    /**
     * Set the list of request/response interceptors.
     * <p>
     * Interceptors are invoked in the order they appear in the list, allowing for
     * controlled processing chains (e.g., authentication before logging).
     * <p>
     * The provided list is copied to prevent external modifications from affecting
     * this configuration.
     *
     * @param interceptors the list of interceptors to use (will be copied)
     * @see ClientTransportConfigBuilder#addInterceptor(ClientCallInterceptor)
     */
    public void setInterceptors(List<ClientCallInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
    }

    /**
     * Get the list of configured interceptors.
     * <p>
     * Returns the internal list of interceptors. Modifications to the returned list
     * will affect this configuration.
     *
     * @return the list of configured interceptors (never null, but may be empty)
     */
    public List<ClientCallInterceptor> getInterceptors() {
        return interceptors;
    }
}
