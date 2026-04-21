package org.a2aproject.sdk.compat03.transport.jsonrpc.context;

/**
 * Shared JSON-RPC context keys for A2A protocol data.
 *
 * These keys provide access to JSON-RPC context information,
 * enabling rich context access in service method implementations.
 */
public final class JSONRPCContextKeys_v0_3 {

    /**
     * Context key for storing the headers.
     */
    public static final String HEADERS_KEY = "headers";

    /**
     * Context key for storing the method name being called.
     */
    public static final String METHOD_NAME_KEY = "method";

    private JSONRPCContextKeys_v0_3() {
        // Utility class
    }
}
