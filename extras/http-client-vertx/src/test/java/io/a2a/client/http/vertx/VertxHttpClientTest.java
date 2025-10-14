package io.a2a.client.http.vertx;

import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.http.common.AbstractHttpClientTest;
import io.vertx.core.http.HttpClientOptions;

public class VertxHttpClientTest extends AbstractHttpClientTest {

    protected HttpClientBuilder getHttpClientBuilder() {
        return new VertxHttpClientBuilder()
                .options(new HttpClientOptions().setMaxChunkSize(24));
    }
}
