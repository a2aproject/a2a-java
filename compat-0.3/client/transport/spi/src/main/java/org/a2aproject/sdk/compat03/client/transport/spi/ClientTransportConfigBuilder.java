package org.a2aproject.sdk.compat03.client.transport.spi;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor;

import java.util.ArrayList;
import java.util.List;

public abstract class ClientTransportConfigBuilder<T extends ClientTransportConfig<? extends ClientTransport>,
        B extends ClientTransportConfigBuilder<T, B>> {

    protected List<ClientCallInterceptor> interceptors = new ArrayList<>();

    public B addInterceptor(ClientCallInterceptor interceptor) {
        if (interceptor != null) {
            this.interceptors.add(interceptor);
        }

        return (B) this;
    }

    public abstract T build();
}
