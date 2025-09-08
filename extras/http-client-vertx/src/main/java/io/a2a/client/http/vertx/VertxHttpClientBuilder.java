package io.a2a.client.http.vertx;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpClientBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;

public class VertxHttpClientBuilder implements HttpClientBuilder {

    private Vertx vertx;

    private HttpClientOptions options;

    public VertxHttpClientBuilder vertx(Vertx vertx) {
        this.vertx = vertx;
        return this;
    }

    public VertxHttpClientBuilder options(HttpClientOptions options) {
        this.options = options;
        return this;
    }

    @Override
    public HttpClient create(String url) {
        return new VertxHttpClient(url,
                vertx != null ? vertx : Vertx.vertx(),
                options != null ? options : new HttpClientOptions());
    }
}
