package io.a2a.client.transport.spi;

import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;

import java.util.List;

/**
 * Configuration for an A2A client transport.
 */
public abstract class ClientTransportConfig<T extends ClientTransport> {

    protected List<ClientCallInterceptor> interceptors;

    public void setInterceptors(List<ClientCallInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public List<ClientCallInterceptor> getInterceptors() {
        return interceptors;
    }
}