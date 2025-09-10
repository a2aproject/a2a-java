package io.a2a.client.transport.rest;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportConfig;

public class RestTransportConfig extends ClientTransportConfig<RestTransport>  {

    private final A2AHttpClient httpClient;

    public RestTransportConfig() {
        this.httpClient = null;
    }

    public RestTransportConfig(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public A2AHttpClient getHttpClient() {
        return httpClient;
    }
}