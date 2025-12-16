package io.a2a.spec;

/**
 * JSON-RPC response containing an agent's extended card with authenticated details.
 * <p>
 * This response returns an {@link AgentCard} with additional information only available
 * to authenticated clients. The extended card may include premium features, detailed
 * security configurations, or other authenticated-only capabilities.
 * <p>
 * If the agent doesn't support authenticated extended cards or authentication fails,
 * the error field will contain a {@link JSONRPCError} such as
 * {@link AuthenticatedExtendedCardNotConfiguredError}.
 *
 * @see GetAuthenticatedExtendedCardRequest for the corresponding request
 * @see AgentCard for the card structure
 * @see AuthenticatedExtendedCardNotConfiguredError for the error when unsupported
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetAuthenticatedExtendedCardResponse extends JSONRPCResponse<AgentCard> {

    /**
     * Constructs response with full parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the agent card result
     * @param error the error if any
     */
    public GetAuthenticatedExtendedCardResponse(String jsonrpc, Object id, AgentCard result, JSONRPCError error) {
        super(jsonrpc, id, result, error, AgentCard.class);
    }

    /**


     * Constructs error response.


     *


     * @param id the request ID


     * @param error the error


     */


    public GetAuthenticatedExtendedCardResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    /**


     * Constructs successful response.


     *


     * @param id the request ID


     * @param result the agent card result


     */


    public GetAuthenticatedExtendedCardResponse(Object id, AgentCard result) {
        this(null, id, result, null);
    }

}
