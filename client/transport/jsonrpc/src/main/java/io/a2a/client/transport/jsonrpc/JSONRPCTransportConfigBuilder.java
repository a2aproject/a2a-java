package io.a2a.client.transport.jsonrpc;

import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import io.a2a.util.Assert;

public class JSONRPCTransportConfigBuilder extends ClientTransportConfigBuilder<JSONRPCTransportConfig, JSONRPCTransportConfigBuilder> {

    private HttpClientBuilder httpClientBuilder = HttpClientBuilder.DEFAULT_FACTORY;

    public JSONRPCTransportConfigBuilder httpClientBuilder(HttpClientBuilder httpClientBuilder) {
        Assert.checkNotNullParam("httpClientBuilder", httpClientBuilder);
        this.httpClientBuilder = httpClientBuilder;

        return this;
    }

    @Override
    public JSONRPCTransportConfig build() {
        JSONRPCTransportConfig config = new JSONRPCTransportConfig(httpClientBuilder);
        config.setInterceptors(this.interceptors);
        return config;
    }
}