package io.a2a.jsonrpc.common.wrappers;

import static io.a2a.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;

import java.util.UUID;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;

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
 * {@link AgentCapabilities#extendedAgentCard()} }) and the client must provide
 * valid authentication credentials for this request to succeed.
 * <p>
 * This class implements the JSON-RPC {@code GetExtendedAgentCard} method
 * as specified in the A2A Protocol.
 *
 * @see GetExtendedAgentCardResponse for the corresponding response
 * @see AgentCard for the card structure
 * @see ExtendedAgentCardNotConfiguredError for the error when unsupported
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class GetExtendedAgentCardRequest extends NonStreamingJSONRPCRequest<Void> {

    /**
     * Constructs request with full parameters.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     */
    public GetExtendedAgentCardRequest(String jsonrpc, Object id) {
        super(jsonrpc, GET_EXTENDED_AGENT_CARD_METHOD, id);
    }

    /**
     * Constructs request with ID only (uses default JSON-RPC version).
     *
     * @param id the request ID
     */
    public GetExtendedAgentCardRequest(String id) {
        this(null, id);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing instances.
     */
    public static class Builder {
        private String jsonrpc;
        private Object id;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the JSON-RPC version.
         *
         * @param jsonrpc the JSON-RPC version
         * @return this builder for method chaining
         */
        public GetExtendedAgentCardRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request ID.
         *
         * @param id the request ID
         * @return this builder for method chaining
         */
        public GetExtendedAgentCardRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return a new instance
         */
        public GetExtendedAgentCardRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetExtendedAgentCardRequest(jsonrpc, id);
        }
    }
}
