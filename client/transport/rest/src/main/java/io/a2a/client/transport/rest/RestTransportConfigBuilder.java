package io.a2a.client.transport.rest;

import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.transport.spi.ClientTransportConfigBuilder;

import io.a2a.util.Assert;

public class RestTransportConfigBuilder extends ClientTransportConfigBuilder<RestTransportConfig, RestTransportConfigBuilder> {

    private HttpClientBuilder httpClientBuilder = io.a2a.client.http.HttpClientBuilder.DEFAULT_FACTORY;

    public RestTransportConfigBuilder httpClientBuilder(HttpClientBuilder httpClientBuilder) {
        Assert.checkNotNullParam("httpClientBuilder", httpClientBuilder);
        this.httpClientBuilder = httpClientBuilder;

        return this;
    }

    @Override
    public RestTransportConfig build() {
        RestTransportConfig config = new RestTransportConfig(this.httpClientBuilder);
        config.setInterceptors(this.interceptors);
        return config;
    }
}