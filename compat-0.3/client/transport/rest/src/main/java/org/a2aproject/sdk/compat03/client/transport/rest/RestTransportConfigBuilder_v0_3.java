package org.a2aproject.sdk.compat03.client.transport.rest;

import org.a2aproject.sdk.compat03.client.http.A2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder_v0_3;
import org.jspecify.annotations.Nullable;

public class RestTransportConfigBuilder_v0_3 extends ClientTransportConfigBuilder_v0_3<RestTransportConfig_v0_3, RestTransportConfigBuilder_v0_3> {

    private @Nullable A2AHttpClient_v0_3 httpClient;

    public RestTransportConfigBuilder_v0_3 httpClient(A2AHttpClient_v0_3 httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    @Override
    public RestTransportConfig_v0_3 build() {
        // No HTTP client provided, fallback to the default one (JDK-based implementation)
        if (httpClient == null) {
            httpClient = new JdkA2AHttpClient_v0_3();
        }

        RestTransportConfig_v0_3 config = new RestTransportConfig_v0_3(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}