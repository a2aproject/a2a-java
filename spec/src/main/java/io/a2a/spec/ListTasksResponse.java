package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The response for a list tasks request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ListTasksResponse extends JSONRPCResponse<ListTasksResult> {

    @JsonCreator
    public ListTasksResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                             @JsonProperty("result") ListTasksResult result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, ListTasksResult.class);
    }

    public ListTasksResponse(Object id, ListTasksResult result) {
        this(null, id, result, null);
    }

    public ListTasksResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
}
