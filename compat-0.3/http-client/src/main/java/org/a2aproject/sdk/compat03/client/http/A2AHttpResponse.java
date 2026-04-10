package org.a2aproject.sdk.compat03.client.http;

public interface A2AHttpResponse {
    int status();

    boolean success();

    String body();
}
