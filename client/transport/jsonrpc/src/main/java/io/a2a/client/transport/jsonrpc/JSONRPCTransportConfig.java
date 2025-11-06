package io.a2a.client.transport.jsonrpc;

import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.client.http.A2AHttpClient;
import org.jspecify.annotations.Nullable;

public class JSONRPCTransportConfig extends ClientTransportConfig<JSONRPCTransport> {

    private final @Nullable A2AHttpClient httpClient;

    public JSONRPCTransportConfig() {
        this.httpClient = null;
    }

    public JSONRPCTransportConfig(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public @Nullable A2AHttpClient getHttpClient() {
        return httpClient;
    }
}