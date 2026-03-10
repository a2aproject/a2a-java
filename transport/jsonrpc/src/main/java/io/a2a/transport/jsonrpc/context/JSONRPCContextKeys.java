package io.a2a.transport.jsonrpc.context;

/**
 * Shared JSON-RPC context keys for A2A protocol data.
 *
 * <p>These keys provide access to JSON-RPC context information stored in
 * {@link io.a2a.server.ServerCallContext}, enabling rich context access
 * in service method implementations and middleware.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public void processRequest(ServerCallContext context) {
 *     String tenant = context.get(JSONRPCContextKeys.TENANT_KEY);
 *     String method = context.get(JSONRPCContextKeys.METHOD_NAME_KEY);
 *     Map<String, String> headers = context.get(JSONRPCContextKeys.HEADERS_KEY);
 * }
 * }</pre>
 *
 * @see io.a2a.server.ServerCallContext
 */
public final class JSONRPCContextKeys {
    
    /**
     * Context key for storing the headers.
     */
    public static final String HEADERS_KEY = "headers";

    /**
     * Context key for storing the method name being called.
     */
    public static final String METHOD_NAME_KEY = "method";

    /**
     * Context key for storing the tenant identifier extracted from the normalized path.
     */
    public static final String TENANT_KEY = "tenant";

    private JSONRPCContextKeys() {
        // Utility class
    }
}
