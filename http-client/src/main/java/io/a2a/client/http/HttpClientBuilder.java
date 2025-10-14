package io.a2a.client.http;

import io.a2a.client.http.jdk.JdkHttpClientBuilder;

public interface HttpClientBuilder {

    HttpClientBuilder DEFAULT_FACTORY = new JdkHttpClientBuilder();

    HttpClient create(String url);
}
