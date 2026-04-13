package org.a2aproject.sdk.compat03.spec;

/**
 * A response for the `agent/getAuthenticatedExtendedCard` method.
 */
public final class GetAuthenticatedExtendedCardResponse extends JSONRPCResponse<AgentCard> {

    public GetAuthenticatedExtendedCardResponse(String jsonrpc, Object id, AgentCard result, JSONRPCError error) {
        super(jsonrpc, id, result, error, AgentCard.class);
    }

    public GetAuthenticatedExtendedCardResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public GetAuthenticatedExtendedCardResponse(Object id, AgentCard result) {
        this(null, id, result, null);
    }

}
