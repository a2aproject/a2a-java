package io.a2a.client.transport.jsonrpc;

import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.client.http.A2AHttpClient;

public class JSONRPCTransportConfig extends ClientTransportConfig<JSONRPCTransport> {

    private final A2AHttpClient httpClient;

    public JSONRPCTransportConfig() {
        this.httpClient = null;
    }

    public JSONRPCTransportConfig(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public A2AHttpClient getHttpClient() {
        return httpClient;
    }
}