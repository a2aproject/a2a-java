package org.a2aproject.sdk.spec;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Client exception indicating an HTTP transport error with a specific status code.
 * <p>
 * This exception is thrown when HTTP communication with an A2A agent fails,
 * capturing both the HTTP status code and error message. It is used for non-2xx
 * HTTP responses that don't contain valid A2A Protocol error responses.
 * <p>
 * Common HTTP status codes:
 * <ul>
 *   <li>4xx - Client errors (400 Bad Request, 401 Unauthorized, 404 Not Found, etc.)</li>
 *   <li>5xx - Server errors (500 Internal Server Error, 503 Service Unavailable, etc.)</li>
 * </ul>
 * <p>
 * This exception is set as the cause of {@link A2AClientException} so that callers
 * can inspect the HTTP status code while remaining backward compatible:
 * <pre>{@code
 * } catch (A2AClientException e) {
 *     if (e.getCause() instanceof A2AClientHTTPError httpError) {
 *         int status = httpError.getCode();           // e.g. 401, 503
 *         String body = httpError.getResponseBody();  // raw response body, may be null
 *     }
 * }
 * }</pre>
 *
 * @see A2AClientError for the base client error class
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">HTTP Status Codes</a>
 */
public class A2AClientHTTPError extends A2AClientError {
    /**
     * The HTTP status code.
     */
    private final int code;

    /**
     * The error message.
     */
    private final String message;

    /**
     * The raw HTTP response body, may be {@code null}.
     */
    @Nullable
    private final String responseBody;

    /**
     * Creates a new HTTP client error with the specified status code and message.
     *
     * @param code the HTTP status code
     * @param message the error message
     * @param data additional error data (may be the response body)
     * @throws IllegalArgumentException if code or message is null
     * @deprecated Use {@link #A2AClientHTTPError(int, String, String)} instead to preserve the response body.
     */
    @Deprecated(since = "1.0.0.Beta1", forRemoval = true)
    public A2AClientHTTPError(int code, String message, Object data) {
        Assert.checkNotNullParam("code", code);
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.message = message;
        this.responseBody = data instanceof String s ? s : null;
    }

    /**
     * Creates a new HTTP client error with the specified status code, message, and response body.
     *
     * @param code the HTTP status code (e.g. 401, 503)
     * @param message the error message
     * @param responseBody the raw HTTP response body, may be {@code null}
     */
    public A2AClientHTTPError(int code, String message, @Nullable String responseBody) {
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.message = message;
        this.responseBody = responseBody;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the HTTP status code (e.g. 401, 404, 500, 503)
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Returns the raw HTTP response body, if available.
     *
     * @return the response body, or {@code null} if not available
     */
    public @Nullable String getResponseBody() {
        return responseBody;
    }
}
