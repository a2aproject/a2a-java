package io.a2a.client.http;

/**
 * HTTP response wrapper containing status code and response body.
 *
 * <p>Provides access to the HTTP status code, a success indicator, and the
 * response body content.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * A2AHttpResponse response = client.createGet()
 *     .url("http://localhost:9999/api/endpoint")
 *     .get();
 *
 * if (response.success()) {
 *     String body = response.body();
 *     // Process successful response
 * } else {
 *     int status = response.status();
 *     // Handle error based on status code
 * }
 * }</pre>
 */
public interface A2AHttpResponse {
    /**
     * Returns the HTTP status code.
     *
     * @return the HTTP status code (e.g., 200, 404, 500)
     */
    int status();

    /**
     * Indicates whether the request was successful.
     *
     * <p>Typically returns {@code true} for 2xx status codes.
     *
     * @return {@code true} if the request was successful, {@code false} otherwise
     */
    boolean success();

    /**
     * Returns the response body content as a string.
     *
     * @return the response body, may be empty but not null
     */
    String body();
}
