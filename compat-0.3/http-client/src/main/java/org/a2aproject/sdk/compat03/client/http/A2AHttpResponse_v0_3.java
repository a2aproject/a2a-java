package org.a2aproject.sdk.compat03.client.http;

public interface A2AHttpResponse_v0_3 {
    int status();

    boolean success();

    String body();
}
