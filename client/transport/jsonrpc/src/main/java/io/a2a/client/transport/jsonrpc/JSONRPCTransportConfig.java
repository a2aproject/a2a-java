package io.a2a.client.transport.jsonrpc;

import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

public class JSONRPCTransportConfig extends ClientTransportConfig<JSONRPCTransport> {

    private final HttpClientBuilder httpClientBuilder;

    public JSONRPCTransportConfig(HttpClientBuilder httpClientBuilder) {
        Assert.checkNotNullParam("httpClientBuilder", httpClientBuilder);
        this.httpClientBuilder = httpClientBuilder;
    }

    public JSONRPCTransportConfig() {
        this.httpClientBuilder = HttpClientBuilder.DEFAULT_FACTORY;
    }

    public HttpClientBuilder getHttpClientBuilder() {
        return this.httpClientBuilder;
    }
}