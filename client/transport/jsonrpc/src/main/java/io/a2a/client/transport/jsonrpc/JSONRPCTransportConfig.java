package io.a2a.client.transport.jsonrpc;

import io.a2a.client.config.ClientTransportConfig;
import io.a2a.client.http.A2AHttpClient;

public class JSONRPCTransportConfig implements ClientTransportConfig {

    private final A2AHttpClient httpClient;

    public JSONRPCTransportConfig(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public A2AHttpClient getHttpClient() {
        return httpClient;
    }
}