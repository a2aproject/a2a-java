package io.a2a.spec;

import java.util.UUID;

/**
 * JSON-RPC request to retrieve an agent's extended card with authenticated details.
 * <p>
 * This request fetches an extended version of the {@link AgentCard} that may contain
 * additional information only available to authenticated clients, such as:
 * <ul>
 *   <li>Additional security scheme details</li>
 *   <li>Extended capability information</li>
 *   <li>Authenticated-only skills or interfaces</li>
 *   <li>Premium or restricted features</li>
 * </ul>
 * <p>
 * The agent must support authenticated extended cards (indicated by
 * {@link AgentCard#supportsAuthenticatedExtendedCard()}) and the client must provide
 * valid authentication credentials for this request to succeed.
 * <p>
 * This class implements the JSON-RPC {@code agent/getAuthenticatedExtendedCard} method
 * as specified in the A2A Protocol.
 *
 * @see GetAuthenticatedExtendedCardResponse for the corresponding response
 * @see AgentCard for the card structure
 * @see AuthenticatedExtendedCardNotConfiguredError for the error when unsupported
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetAuthenticatedExtendedCardRequest extends NonStreamingJSONRPCRequest<Void> {

    public static final String METHOD = "GetExtendedAgentCard";

    public GetAuthenticatedExtendedCardRequest(String jsonrpc, Object id) {
        super(jsonrpc, METHOD, id);
    }

    public GetAuthenticatedExtendedCardRequest(String id) {
        this(null, id);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;

        public GetAuthenticatedExtendedCardRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetAuthenticatedExtendedCardRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * @deprecated
         */
        public GetAuthenticatedExtendedCardRequest.Builder method(String method) {
            return this;
        }

        public GetAuthenticatedExtendedCardRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetAuthenticatedExtendedCardRequest(jsonrpc, id);
        }
    }
}
