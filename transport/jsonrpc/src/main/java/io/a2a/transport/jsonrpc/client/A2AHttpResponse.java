package io.a2a.transport.jsonrpc.client;

public interface A2AHttpResponse {
    int status();

    boolean success();

    String body();
}
