package io.a2a.client.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface A2AHttpClient {

    String CONTENT_TYPE= "Content-Type";
    String APPLICATION_JSON= "application/json";
    String ACCEPT = "Accept";
    String EVENT_STREAM = "text/event-stream";

    GetBuilder createGet();

    PostBuilder createPost();

    DeleteBuilder createDelete();

    interface Builder<T extends Builder<T>> {
        T url(String s);
        T addHeaders(Map<String, String> headers);
        T addHeader(String name, String value);
    }

    interface GetBuilder extends Builder<GetBuilder> {
        A2AHttpResponse get() throws IOException, InterruptedException;
        CompletableFuture<Void> getAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException;
    }

    interface PostBuilder extends Builder<PostBuilder> {
        PostBuilder body(String body);
        A2AHttpResponse post() throws IOException, InterruptedException;
        CompletableFuture<Void> postAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException;
    }

    interface DeleteBuilder extends Builder<DeleteBuilder> {
        A2AHttpResponse delete() throws IOException, InterruptedException;
    }
}
