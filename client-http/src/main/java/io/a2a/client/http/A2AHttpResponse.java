package io.a2a.client.http;

public interface A2AHttpResponse {
    int status();

    boolean success();

    String body();
}
