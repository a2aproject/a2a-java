package org.a2aproject.sdk.transport.jsonrpc.context;

/**
 * Shared JSON-RPC context keys for A2A protocol data.
 *
 * <p>These keys provide access to JSON-RPC context information stored in
 * {@link org.a2aproject.sdk.server.ServerCallContext}, enabling rich context access
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
 * @see org.a2aproject.sdk.server.ServerCallContext
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
     * Context key for storing the tenant identifier extracted from the URL path.
     *
     * <p><b>Note:</b> This key stores the URL path tenant (e.g. {@code "acme"} for
     * {@code POST /acme}). When the effective tenant comes solely from the JSON-RPC
     * {@code params} body (i.e. the URL path is {@code /} but {@code params.tenant} is
     * non-blank), this key will be an empty string. Use
     * {@link org.a2aproject.sdk.server.agentexecution.RequestContext#getTenant()} as the
     * authoritative source for the effective tenant in handler and executor code.
     */
    public static final String TENANT_KEY = "tenant";

    private JSONRPCContextKeys() {
        // Utility class
    }
}
