package io.a2a.client.http;

import io.a2a.client.http.sse.Event;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface HttpResponse {
    int statusCode();

    default boolean success() {
        return statusCode() >= 200 && statusCode() < 300;
    }

    CompletableFuture<String> body();

    void bodyAsSse(Consumer<Event> eventConsumer, Consumer<Throwable> errorConsumer);
}
