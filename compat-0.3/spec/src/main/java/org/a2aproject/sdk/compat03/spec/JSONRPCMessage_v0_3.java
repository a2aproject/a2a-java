package org.a2aproject.sdk.compat03.spec;

/**
 * Defines the base structure for any JSON-RPC 2.0 request, response, or notification.
 */
public sealed interface JSONRPCMessage_v0_3 permits JSONRPCRequest_v0_3, JSONRPCResponse_v0_3 {

    String JSONRPC_VERSION = "2.0";

    String getJsonrpc();
    Object getId();

}
