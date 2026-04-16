package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import org.a2aproject.sdk.compat03.client.http.A2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder_v0_3;

public class JSONRPCTransportConfigBuilder_v0_3 extends ClientTransportConfigBuilder_v0_3<JSONRPCTransportConfig_v0_3, JSONRPCTransportConfigBuilder_v0_3> {

    private A2AHttpClient_v0_3 httpClient;

    public JSONRPCTransportConfigBuilder_v0_3 httpClient(A2AHttpClient_v0_3 httpClient) {
        this.httpClient = httpClient;

        return this;
    }

    @Override
    public JSONRPCTransportConfig_v0_3 build() {
        // No HTTP client provided, fallback to the default one (JDK-based implementation)
        if (httpClient == null) {
            httpClient = new JdkA2AHttpClient_v0_3();
        }

        JSONRPCTransportConfig_v0_3 config = new JSONRPCTransportConfig_v0_3(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}