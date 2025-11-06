package io.a2a.client.transport.jsonrpc;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import org.jspecify.annotations.Nullable;

public class JSONRPCTransportConfigBuilder extends ClientTransportConfigBuilder<JSONRPCTransportConfig, JSONRPCTransportConfigBuilder> {

    private @Nullable A2AHttpClient httpClient;

    public JSONRPCTransportConfigBuilder httpClient(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    @Override
    public JSONRPCTransportConfig build() {
        // No HTTP client provided, fallback to the default one (JDK-based implementation)
        if (httpClient == null) {
            httpClient = new JdkA2AHttpClient();
        }

        JSONRPCTransportConfig config = new JSONRPCTransportConfig(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}