package org.a2aproject.sdk.compat03.spec;

/**
 * A response for the `agent/getAuthenticatedExtendedCard` method.
 */
public final class GetAuthenticatedExtendedCardResponse_v0_3 extends JSONRPCResponse_v0_3<AgentCard_v0_3> {

    public GetAuthenticatedExtendedCardResponse_v0_3(String jsonrpc, Object id, AgentCard_v0_3 result, JSONRPCError_v0_3 error) {
        super(jsonrpc, id, result, error, AgentCard_v0_3.class);
    }

    public GetAuthenticatedExtendedCardResponse_v0_3(Object id, JSONRPCError_v0_3 error) {
        this(null, id, null, error);
    }

    public GetAuthenticatedExtendedCardResponse_v0_3(Object id, AgentCard_v0_3 result) {
        this(null, id, result, null);
    }

}
