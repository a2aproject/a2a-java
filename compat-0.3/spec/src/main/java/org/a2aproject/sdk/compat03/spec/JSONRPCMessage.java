package org.a2aproject.sdk.compat03.spec;

/**
 * Defines the base structure for any JSON-RPC 2.0 request, response, or notification.
 */
public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCResponse {

    String JSONRPC_VERSION = "2.0";

    String getJsonrpc();
    Object getId();

}
