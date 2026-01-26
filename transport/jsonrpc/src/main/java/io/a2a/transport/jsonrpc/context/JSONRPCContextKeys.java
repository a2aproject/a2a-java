package io.a2a.transport.jsonrpc.context;

/**
 * Shared JSON-RPC context keys for A2A protocol data.
 *
 * These keys provide access to JSON-RPC context information,
 * enabling rich context access in service method implementations.
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
