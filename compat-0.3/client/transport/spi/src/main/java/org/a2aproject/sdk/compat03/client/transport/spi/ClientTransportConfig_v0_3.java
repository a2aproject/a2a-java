package org.a2aproject.sdk.compat03.client.transport.spi;

import java.util.ArrayList;
import java.util.List;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallInterceptor_v0_3;

/**
 * Configuration for an A2A client transport.
 */
public abstract class ClientTransportConfig_v0_3<T extends ClientTransport_v0_3> {

    protected List<ClientCallInterceptor_v0_3> interceptors = new ArrayList<>();

    public void setInterceptors(List<ClientCallInterceptor_v0_3> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
    }

    public List<ClientCallInterceptor_v0_3> getInterceptors() {
        return interceptors;
    }
}
