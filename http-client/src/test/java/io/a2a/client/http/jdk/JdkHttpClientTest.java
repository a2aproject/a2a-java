package io.a2a.client.http.jdk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JdkHttpClientTest {

    @Test
    public void testBaseUrlNormalization() {
        String baseUrl = "http://localhost:8080";

        JdkHttpClient client = new JdkHttpClient(baseUrl);
        Assertions.assertEquals(baseUrl, client.getBaseUrl());

        baseUrl = "http://localhost";
        client = new JdkHttpClient(baseUrl);
        Assertions.assertEquals("http://localhost", client.getBaseUrl());

        baseUrl = "https://localhost";
        client = new JdkHttpClient(baseUrl);
        Assertions.assertEquals("https://localhost", client.getBaseUrl());

        baseUrl = "https://localhost:443";
        client = new JdkHttpClient(baseUrl);
        Assertions.assertEquals("https://localhost:443", client.getBaseUrl());

        baseUrl = "https://localhost:80/test";
        client = new JdkHttpClient(baseUrl);
        Assertions.assertEquals("https://localhost:80", client.getBaseUrl());
    }
}