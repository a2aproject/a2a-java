package org.a2aproject.sdk.client.http.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpHeaders;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.client.http.ServerSentEventParser;
import org.a2aproject.sdk.common.A2AErrorMessages;
import org.a2aproject.sdk.spec.A2AClientHTTPError;

/**
 * Test-only {@link A2AHttpClient} backed by OkHttp, simulating Android's HTTP stack.
 *
 * <p>Android uses OkHttp internally for {@link java.net.HttpURLConnection}. This class
 * exercises the same redirect and header-forwarding semantics without requiring a real
 * Android runtime, allowing the shared abstract test suites to validate behaviour on
 * both JDK and OkHttp.
 */
class OkHttpA2AHttpClient implements A2AHttpClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int HTTP_OK = 200;
    private static final int HTTP_MULT_CHOICE = 300;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;

    private static final Executor NET_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "A2A-OkHttp-Test-Net");
        t.setDaemon(true);
        return t;
    });

    @Override
    public GetBuilder createGet() {
        return new OkHttpGetBuilder();
    }

    @Override
    public PostBuilder createPost() {
        return new OkHttpPostBuilder();
    }

    @Override
    public DeleteBuilder createDelete() {
        return new OkHttpDeleteBuilder();
    }

    private abstract static class OkHttpBuilder<T extends Builder<T>> implements Builder<T> {
        protected String url = "";
        protected final Map<String, String> headers = new HashMap<>();

        @Override
        public T url(String url) {
            this.url = url;
            return self();
        }

        @Override
        public T addHeader(String name, String value) {
            headers.put(name, value);
            return self();
        }

        @Override
        public T addHeaders(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return self();
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        protected OkHttpClient buildClient(boolean followRedirects) {
            return new OkHttpClient.Builder()
                    .followRedirects(followRedirects)
                    .followSslRedirects(followRedirects)
                    .build();
        }

        protected Request.Builder applyHeaders(Request.Builder builder) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
            return builder;
        }

        protected A2AHttpResponse toResponse(Response response) throws IOException {
            int status = response.code();
            A2AHttpHeaders responseHeaders = fromOkHttpHeaders(response);

            if (status == HTTP_UNAUTHORIZED) {
                throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED,
                        new A2AClientHTTPError(HTTP_UNAUTHORIZED, A2AErrorMessages.AUTHENTICATION_FAILED,
                                null, responseHeaders.toMap()));
            } else if (status == HTTP_FORBIDDEN) {
                throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED,
                        new A2AClientHTTPError(HTTP_FORBIDDEN, A2AErrorMessages.AUTHORIZATION_FAILED,
                                null, responseHeaders.toMap()));
            }

            ResponseBody body = response.body();
            String bodyStr = body != null ? body.string() : "";
            return new OkHttpResponse(status, bodyStr, responseHeaders);
        }

        protected void processSSEResponse(
                Response response,
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) {
            try {
                if (handleErrorStatus(response, messageConsumer, errorConsumer)) {
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    completeRunnable.run();
                    return;
                }

                String contentType = response.header("Content-Type");
                boolean isSse = contentType != null && contentType.contains(EVENT_STREAM);
                parseResponseBody(body, isSse, messageConsumer, errorConsumer);
                completeRunnable.run();
            } catch (Exception e) {
                errorConsumer.accept(e);
            } finally {
                response.close();
            }
        }

        private boolean handleErrorStatus(
                Response response,
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer) throws IOException {
            int status = response.code();
            if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
                A2AHttpHeaders responseHeaders = fromOkHttpHeaders(response);
                String msg = status == HTTP_UNAUTHORIZED
                        ? A2AErrorMessages.AUTHENTICATION_FAILED
                        : A2AErrorMessages.AUTHORIZATION_FAILED;
                errorConsumer.accept(new IOException(msg,
                        new A2AClientHTTPError(status, msg, null, responseHeaders.toMap())));
                return true;
            }

            if (!(status >= HTTP_OK && status < HTTP_MULT_CHOICE)) {
                ResponseBody body = response.body();
                String errorBody = body != null ? body.string() : "";
                if (!errorBody.isEmpty()) {
                    messageConsumer.accept(new ServerSentEvent(errorBody));
                } else {
                    errorConsumer.accept(new IOException("Request failed with status " + status));
                }
                return true;
            }
            return false;
        }

        private void parseResponseBody(
                ResponseBody body,
                boolean isSse,
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(body.byteStream(), StandardCharsets.UTF_8))) {
                String line;
                if (isSse) {
                    ServerSentEventParser sseParser = new ServerSentEventParser(messageConsumer, errorConsumer);
                    while ((line = reader.readLine()) != null) {
                        sseParser.processLine(line);
                    }
                    sseParser.flush();
                } else {
                    StringBuilder bodyBuffer = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (!line.isEmpty()) {
                            if (bodyBuffer.length() > 0) {
                                bodyBuffer.append('\n');
                            }
                            bodyBuffer.append(line);
                        }
                    }
                    String result = bodyBuffer.toString();
                    if (!result.isEmpty()) {
                        messageConsumer.accept(new ServerSentEvent(result));
                    }
                }
            }
        }
    }

    private static class OkHttpGetBuilder extends OkHttpBuilder<GetBuilder> implements GetBuilder {
        @Override
        public A2AHttpResponse get() throws IOException {
            OkHttpClient client = buildClient(false);
            Request request = applyHeaders(new Request.Builder().url(url).get()).build();
            try (Response response = client.newCall(request).execute()) {
                return toResponse(response);
            }
        }

        @Override
        public CompletableFuture<Void> getAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) {
            return CompletableFuture.runAsync(() -> {
                try {
                    OkHttpClient client = buildClient(false);
                    Request request = applyHeaders(
                            new Request.Builder().url(url).get()
                                    .addHeader(ACCEPT, EVENT_STREAM))
                            .build();
                    Response response = client.newCall(request).execute();
                    processSSEResponse(response, messageConsumer, errorConsumer, completeRunnable);
                } catch (Exception e) {
                    errorConsumer.accept(e);
                }
            }, NET_EXECUTOR);
        }
    }

    private static class OkHttpPostBuilder extends OkHttpBuilder<PostBuilder> implements PostBuilder {
        private String body = "";
        private boolean followRedirects = false;

        @Override
        public PostBuilder body(String body) {
            this.body = body;
            return self();
        }

        @Override
        public PostBuilder followRedirects(boolean follow) {
            this.followRedirects = follow;
            return self();
        }

        @Override
        public A2AHttpResponse post() throws IOException {
            OkHttpClient client = buildClient(followRedirects);
            Request request = applyHeaders(
                    new Request.Builder().url(url)
                            .post(RequestBody.create(body, JSON)))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return toResponse(response);
            }
        }

        @Override
        public CompletableFuture<Void> postAsyncSSE(
                Consumer<ServerSentEvent> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) {
            return CompletableFuture.runAsync(() -> {
                try {
                    OkHttpClient client = buildClient(false);
                    Request request = applyHeaders(
                            new Request.Builder().url(url)
                                    .post(RequestBody.create(body, JSON))
                                    .addHeader(ACCEPT, EVENT_STREAM))
                            .build();
                    Response response = client.newCall(request).execute();
                    processSSEResponse(response, messageConsumer, errorConsumer, completeRunnable);
                } catch (Exception e) {
                    errorConsumer.accept(e);
                }
            }, NET_EXECUTOR);
        }
    }

    private static class OkHttpDeleteBuilder extends OkHttpBuilder<DeleteBuilder> implements DeleteBuilder {
        @Override
        public A2AHttpResponse delete() throws IOException {
            OkHttpClient client = buildClient(false);
            Request request = applyHeaders(new Request.Builder().url(url).delete()).build();
            try (Response response = client.newCall(request).execute()) {
                return toResponse(response);
            }
        }
    }

    private static A2AHttpHeaders fromOkHttpHeaders(Response response) {
        Map<String, List<String>> headerMap = new HashMap<>();
        for (String name : response.headers().names()) {
            headerMap.put(name, response.headers().values(name));
        }
        return A2AHttpHeaders.of(headerMap);
    }

    private record OkHttpResponse(int status, String body, A2AHttpHeaders headers) implements A2AHttpResponse {
        @Override
        public boolean success() {
            return status >= HTTP_OK && status < HTTP_MULT_CHOICE;
        }
    }
}
