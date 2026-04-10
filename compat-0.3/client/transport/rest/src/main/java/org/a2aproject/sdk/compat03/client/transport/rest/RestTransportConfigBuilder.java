package org.a2aproject.sdk.compat03.client.transport.rest;

import org.a2aproject.sdk.compat03.client.http.A2AHttpClient;
import org.a2aproject.sdk.compat03.client.http.JdkA2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder;
import org.jspecify.annotations.Nullable;

public class RestTransportConfigBuilder extends ClientTransportConfigBuilder<RestTransportConfig, RestTransportConfigBuilder> {

    private @Nullable A2AHttpClient httpClient;

    public RestTransportConfigBuilder httpClient(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    @Override
    public RestTransportConfig build() {
        // No HTTP client provided, fallback to the default one (JDK-based implementation)
        if (httpClient == null) {
            httpClient = new JdkA2AHttpClient();
        }

        RestTransportConfig config = new RestTransportConfig(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}