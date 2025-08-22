package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A response for the `agent/getAuthenticatedExtendedCard` method.
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
