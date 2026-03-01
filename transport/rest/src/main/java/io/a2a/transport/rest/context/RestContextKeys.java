package io.a2a.transport.rest.context;

/**
 * Shared REST context keys for A2A protocol data.
 *
 * <p>These keys provide access to REST context information stored in
 * {@link io.a2a.server.ServerCallContext}, enabling rich context access
 * in service method implementations and middleware.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public void processRequest(ServerCallContext context) {
 *     String tenant = context.get(RestContextKeys.TENANT_KEY);
 *     String method = context.get(RestContextKeys.METHOD_NAME_KEY);
 *     Map<String, String> headers = context.get(RestContextKeys.HEADERS_KEY);
 * }
 * }</pre>
 *
 * @see io.a2a.server.ServerCallContext
 */
public final class RestContextKeys {
    
    /**
     * Context key for storing the headers.
     */
    public static final String HEADERS_KEY = "headers";

    /**
     * Context key for storing the method name being called.
     */
    public static final String METHOD_NAME_KEY = "method";
    /**
     * Context key for storing the tenant identifier extracted from the request path.
     */
    public static final String TENANT_KEY = "tenant";

    private RestContextKeys() {
        // Utility class
    }
}
