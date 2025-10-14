package io.a2a.client.http.jdk;

import io.a2a.client.http.HttpClient;
import io.a2a.client.http.HttpClientBuilder;

public class JdkHttpClientBuilder implements HttpClientBuilder {

    @Override
    public HttpClient create(String url) {
        return new JdkHttpClient(url);
    }
}
