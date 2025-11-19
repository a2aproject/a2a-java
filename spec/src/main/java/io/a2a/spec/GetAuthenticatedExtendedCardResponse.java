package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetAuthenticatedExtendedCardResponse extends JSONRPCResponse<AgentCard> {

    @JsonCreator
    public GetAuthenticatedExtendedCardResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                @JsonProperty("result") AgentCard result,
                                                @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, AgentCard.class);
    }

    public GetAuthenticatedExtendedCardResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    public GetAuthenticatedExtendedCardResponse(Object id, AgentCard result) {
        this(null, id, result, null);
    }

}
