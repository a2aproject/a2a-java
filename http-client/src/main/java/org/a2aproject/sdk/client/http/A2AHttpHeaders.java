package org.a2aproject.sdk.client.http;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.util.HttpHeaderUtils;
import org.jspecify.annotations.Nullable;

/**
 * Read-only abstraction over HTTP response headers.
 *
 * <p>Header names are case-insensitive per RFC 7230. Implementations must
 * perform case-insensitive lookup for all accessor methods.
 *
 * <p>HTTP headers may have multiple values for the same name (e.g. Set-Cookie).
 * Use {@link #allValues(String)} to retrieve all values, or {@link #firstValue(String)}
 * when only a single value is expected (e.g. Retry-After, Content-Type).
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * A2AHttpResponse response = client.createPost()
 *     .url("http://localhost:9999/message:send")
 *     .body(jsonBody)
 *     .post();
 *
 * if (!response.success()) {
 *     String retryAfter = response.headers().firstValue("Retry-After");
 *     // Handle rate limiting
 * }
 * }</pre>
 *
 * @see A2AHttpResponse#headers()
 */
public interface A2AHttpHeaders {

    /**
     * Empty headers instance returned by default when headers are not available.
     */
    A2AHttpHeaders EMPTY = new A2AHttpHeaders() {
        @Override
        public @Nullable String firstValue(String name) {
            return null;
        }

        @Override
        public List<String> allValues(String name) {
            return List.of();
        }

        @Override
        public Map<String, List<String>> toMap() {
            return Map.of();
        }
    };

    /**
     * Returns the first value for the given header name, or {@code null} if not present.
     *
     * @param name the header name (case-insensitive)
     * @return the first header value, or {@code null}
     */
    @Nullable
    String firstValue(String name);

    /**
     * Returns all values for the given header name.
     *
     * @param name the header name (case-insensitive)
     * @return an unmodifiable list of values, empty if the header is not present
     */
    List<String> allValues(String name);

    /**
     * Returns an unmodifiable map of all headers.
     *
     * <p>The keys in the returned map are in their original casing from the response.
     *
     * @return map of header names to lists of values
     */
    Map<String, List<String>> toMap();

    /**
     * Creates an {@link A2AHttpHeaders} instance from a map of header name to value lists.
     *
     * <p>Builds a case-insensitive snapshot: null keys and null value lists are silently
     * skipped (e.g. the {@code null} status-line key from {@link java.net.HttpURLConnection}).
     *
     * @param headers the source header map; may be null-keyed
     * @return an immutable, case-insensitive {@link A2AHttpHeaders} view
     */
    static A2AHttpHeaders of(Map<String, List<String>> headers) {
        Map<String, List<String>> immutable = HttpHeaderUtils.copyOfCaseInsensitive(headers);
        return new A2AHttpHeaders() {
            @Override
            public @Nullable String firstValue(String name) {
                if (name == null) {
                    return null;
                }
                List<String> values = immutable.get(name);
                return (values != null && !values.isEmpty()) ? values.get(0) : null;
            }

            @Override
            public List<String> allValues(String name) {
                if (name == null) {
                    return List.of();
                }
                List<String> values = immutable.get(name);
                return values != null ? values : List.of();
            }

            @Override
            public Map<String, List<String>> toMap() {
                return immutable;
            }
        };
    }
}
