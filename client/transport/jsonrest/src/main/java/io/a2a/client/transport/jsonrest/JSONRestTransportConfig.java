package io.a2a.client.transport.jsonrest;

import io.a2a.client.config.ClientTransportConfig;
import io.a2a.client.http.A2AHttpClient;

public class JSONRestTransportConfig implements ClientTransportConfig {

    private final A2AHttpClient httpClient;

    public JSONRestTransportConfig(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public A2AHttpClient getHttpClient() {
        return httpClient;
    }
}