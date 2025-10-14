package io.a2a.client.http;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface HttpClient {

    static HttpClient createHttpClient(String baseUrl) {
        return HttpClientBuilder.DEFAULT_FACTORY.create(baseUrl);
    }

    GetRequestBuilder get(String path);

    PostRequestBuilder post(String path);

    DeleteRequestBuilder delete(String path);

    interface RequestBuilder<T extends RequestBuilder<T>> {
        CompletableFuture<HttpResponse> send();

        T addHeader(String name, String value);

        T addHeaders(Map<String, String> headers);
    }

    interface GetRequestBuilder extends RequestBuilder<GetRequestBuilder> {

    }

    interface PostRequestBuilder extends RequestBuilder<PostRequestBuilder> {
        PostRequestBuilder body(@Nullable String body);

        default PostRequestBuilder asSSE() {
            return addHeader("Accept", "text/event-stream");
        }

        default CompletableFuture<HttpResponse> send(String body) {
            return this.body(body).send();
        }
    }

    interface DeleteRequestBuilder extends RequestBuilder<DeleteRequestBuilder> {

    }
}
