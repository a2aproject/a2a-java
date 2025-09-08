package io.a2a.client.transport.rest;

import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.util.Assert;

public class RestTransportConfig extends ClientTransportConfig<RestTransport>  {

    private final HttpClientBuilder httpClientBuilder;

    public RestTransportConfig(HttpClientBuilder httpClientBuilder) {
        Assert.checkNotNullParam("httpClientBuilder", httpClientBuilder);
        this.httpClientBuilder = httpClientBuilder;
    }

    public RestTransportConfig() {
        this.httpClientBuilder = HttpClientBuilder.DEFAULT_FACTORY;
    }

    public HttpClientBuilder getHttpClientBuilder() {
        return httpClientBuilder;
    }
}