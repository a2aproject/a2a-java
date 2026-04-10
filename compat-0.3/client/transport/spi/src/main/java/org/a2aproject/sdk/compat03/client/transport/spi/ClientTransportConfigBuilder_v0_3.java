package org.a2aproject.sdk.compat03.client.transport.spi;

import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;

import java.util.ArrayList;
import java.util.List;

public abstract class ClientTransportConfigBuilder_v0_3<T extends ClientTransportConfig_v0_3<? extends ClientTransport_v0_3>,
        B extends ClientTransportConfigBuilder_v0_3<T, B>> {

    protected List<ClientCallInterceptor_v0_3> interceptors = new ArrayList<>();

    public B addInterceptor(ClientCallInterceptor_v0_3 interceptor) {
        if (interceptor != null) {
            this.interceptors.add(interceptor);
        }

        return (B) this;
    }

    public abstract T build();
}
