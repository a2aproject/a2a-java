package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import org.a2aproject.sdk.compat03.client.http.A2AHttpClient;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder;

public class JSONRPCTransportConfigBuilder extends ClientTransportConfigBuilder<JSONRPCTransportConfig, JSONRPCTransportConfigBuilder> {

    private A2AHttpClient httpClient;

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