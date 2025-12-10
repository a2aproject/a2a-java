package io.a2a.spec;

import io.a2a.util.Assert;

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
 * Usage example:
 * <pre>{@code
 * if (response.statusCode() >= 400) {
 *     throw new A2AClientHTTPError(
 *         response.statusCode(),
 *         "HTTP error: " + response.statusMessage(),
 *         response.body()
 *     );
 * }
 * }</pre>
 *
 * @see A2AClientError for the base client error class
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">HTTP Status Codes</a>
 */
public class A2AClientHTTPError extends A2AClientError {
    private final int code;
    private final String message;

    /**
     * Creates a new HTTP client error with the specified status code and message.
     *
     * @param code the HTTP status code
     * @param message the error message
     * @param data additional error data (may be the response body)
     * @throws IllegalArgumentException if code or message is null
     */
    public A2AClientHTTPError(int code, String message, Object data) {
        Assert.checkNotNullParam("code", code);
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.message = message;
    }

    /**
     * Gets the error code
     *
     * @return the error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the error message
     *
     * @return the error message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
