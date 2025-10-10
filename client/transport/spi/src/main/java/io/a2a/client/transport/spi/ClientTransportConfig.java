package io.a2a.client.transport.spi;

import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import java.util.ArrayList;

import java.util.List;

/**
 * Configuration for an A2A client transport.
 */
public abstract class ClientTransportConfig<T extends ClientTransport> {

    protected List<ClientCallInterceptor> interceptors = new ArrayList<>();

    public void setInterceptors(List<ClientCallInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
    }

    public List<ClientCallInterceptor> getInterceptors() {
        return interceptors;
    }
}