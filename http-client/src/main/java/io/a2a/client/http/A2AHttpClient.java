package io.a2a.client.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * HTTP client interface for making HTTP requests to A2A agents.
 *
 * <p>Provides a fluent builder API for constructing and executing HTTP requests
 * with support for GET, POST, and DELETE methods. Includes support for both
 * synchronous requests and asynchronous Server-Sent Events (SSE) streaming.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * A2AHttpClient client = A2AHttpClientFactory.create();
 *
 * // Synchronous GET request
 * A2AHttpResponse response = client.createGet()
 *     .url("http://localhost:9999/api/endpoint")
 *     .addHeader("Authorization", "Bearer token")
 *     .get();
 *
 * // Synchronous POST request
 * A2AHttpResponse response = client.createPost()
 *     .url("http://localhost:9999/message:send")
 *     .body("{\"message\": \"Hello\"}")
 *     .post();
 *
 * // Asynchronous SSE streaming
 * CompletableFuture<Void> future = client.createPost()
 *     .url("http://localhost:9999/message:stream")
 *     .body(jsonBody)
 *     .postAsyncSSE(
 *         message -> System.out.println("Event: " + message),
 *         error -> System.err.println("Error: " + error),
 *         () -> System.out.println("Stream complete")
 *     );
 * }</pre>
 *
 * @see A2AHttpClientFactory
 * @see A2AHttpResponse
 */
public interface A2AHttpClient {

    /** HTTP Content-Type header name. */
    String CONTENT_TYPE= "Content-Type";
    /** JSON content type value. */
    String APPLICATION_JSON= "application/json";
    /** HTTP Accept header name. */
    String ACCEPT = "Accept";
    /** SSE event stream content type. */
    String EVENT_STREAM = "text/event-stream";

    /**
     * Creates a builder for GET requests.
     *
     * @return a new GetBuilder instance
     */
    GetBuilder createGet();

    /**
     * Creates a builder for POST requests.
     *
     * @return a new PostBuilder instance
     */
    PostBuilder createPost();

    /**
     * Creates a builder for DELETE requests.
     *
     * @return a new DeleteBuilder instance
     */
    DeleteBuilder createDelete();

    /**
     * Base builder interface for HTTP requests.
     *
     * @param <T> the concrete builder type for method chaining
     */
    interface Builder<T extends Builder<T>> {
        /**
         * Sets the target URL for the request.
         *
         * @param s the URL string
         * @return this builder for chaining
         */
        T url(String s);

        /**
         * Adds multiple HTTP headers to the request.
         *
         * @param headers map of header names to values
         * @return this builder for chaining
         */
        T addHeaders(Map<String, String> headers);

        /**
         * Adds a single HTTP header to the request.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder for chaining
         */
        T addHeader(String name, String value);
    }

    /**
     * Builder for HTTP GET requests.
     *
     * <p>Supports both synchronous requests and asynchronous Server-Sent Events (SSE) streaming.
     */
    interface GetBuilder extends Builder<GetBuilder> {
        /**
         * Executes a synchronous GET request.
         *
         * @return the HTTP response
         * @throws IOException if an I/O error occurs
         * @throws InterruptedException if the operation is interrupted
         */
        A2AHttpResponse get() throws IOException, InterruptedException;

        /**
         * Executes an asynchronous GET request expecting Server-Sent Events (SSE).
         *
         * <p>The request will stream SSE messages asynchronously, invoking the provided
         * consumers for each event, error, or completion.
         *
         * @param messageConsumer callback for each SSE message received
         * @param errorConsumer callback for errors during streaming
         * @param completeRunnable callback when the stream completes normally
         * @return a CompletableFuture that completes when streaming ends
         * @throws IOException if an I/O error occurs
         * @throws InterruptedException if the operation is interrupted
         */
        CompletableFuture<Void> getAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException;
    }

    /**
     * Builder for HTTP POST requests.
     *
     * <p>Supports both synchronous requests and asynchronous Server-Sent Events (SSE) streaming.
     */
    interface PostBuilder extends Builder<PostBuilder> {
        /**
         * Sets the request body content.
         *
         * @param body the request body string (typically JSON)
         * @return this builder for chaining
         */
        PostBuilder body(String body);

        /**
         * Executes a synchronous POST request.
         *
         * @return the HTTP response
         * @throws IOException if an I/O error occurs
         * @throws InterruptedException if the operation is interrupted
         */
        A2AHttpResponse post() throws IOException, InterruptedException;

        /**
         * Executes an asynchronous POST request expecting Server-Sent Events (SSE).
         *
         * <p>The request will stream SSE messages asynchronously, invoking the provided
         * consumers for each event, error, or completion.
         *
         * @param messageConsumer callback for each SSE message received
         * @param errorConsumer callback for errors during streaming
         * @param completeRunnable callback when the stream completes normally
         * @return a CompletableFuture that completes when streaming ends
         * @throws IOException if an I/O error occurs
         * @throws InterruptedException if the operation is interrupted
         */
        CompletableFuture<Void> postAsyncSSE(
                Consumer<String> messageConsumer,
                Consumer<Throwable> errorConsumer,
                Runnable completeRunnable) throws IOException, InterruptedException;
    }

    /**
     * Builder for HTTP DELETE requests.
     */
    interface DeleteBuilder extends Builder<DeleteBuilder> {
        /**
         * Executes a synchronous DELETE request.
         *
         * @return the HTTP response
         * @throws IOException if an I/O error occurs
         * @throws InterruptedException if the operation is interrupted
         */
        A2AHttpResponse delete() throws IOException, InterruptedException;
    }
}
