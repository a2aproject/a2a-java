package io.a2a.client.transport.rest;

import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import org.jspecify.annotations.Nullable;

public class RestTransportConfigBuilder extends ClientTransportConfigBuilder<RestTransportConfig, RestTransportConfigBuilder> {

    private @Nullable A2AHttpClient httpClient;

    public RestTransportConfigBuilder httpClient(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    @Override
    public RestTransportConfig build() {
        if (httpClient == null) {
            httpClient = A2AHttpClientFactory.create();
        }

        RestTransportConfig config = new RestTransportConfig(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}