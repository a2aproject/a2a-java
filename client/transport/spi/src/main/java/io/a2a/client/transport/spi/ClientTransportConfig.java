package io.a2a.client.transport.spi;

import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

/**
 * Configuration for an A2A client transport.
 */
public abstract class ClientTransportConfig<T extends ClientTransport> {

    protected List<ClientCallInterceptor> interceptors = new ArrayList<>();
    protected Map<String, ? extends Object > parameters = new HashMap<>();

    public void setInterceptors(List<ClientCallInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
    }

    public List<ClientCallInterceptor> getInterceptors() {
        return interceptors;
    }

    public void setParameters(Map<String, ? extends Object > parameters) {
        this.parameters = new HashMap<>(parameters);
    }

    public Map<String, ? extends Object > getParameters() {
        return parameters;
    }
}