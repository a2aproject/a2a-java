package org.a2aproject.sdk.compat03.client.transport.rest;

import org.a2aproject.sdk.compat03.client.http.A2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfig;
import org.jspecify.annotations.Nullable;

public class RestTransportConfig extends ClientTransportConfig<RestTransport>  {

    private final @Nullable A2AHttpClient httpClient;

    public RestTransportConfig() {
        this.httpClient = null;
    }

    public RestTransportConfig(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public @Nullable A2AHttpClient getHttpClient() {
        return httpClient;
    }
}