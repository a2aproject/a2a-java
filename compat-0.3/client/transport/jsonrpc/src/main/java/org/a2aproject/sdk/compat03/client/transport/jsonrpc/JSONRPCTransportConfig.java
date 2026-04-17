package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.compat03.client.http.A2AHttpClient;

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