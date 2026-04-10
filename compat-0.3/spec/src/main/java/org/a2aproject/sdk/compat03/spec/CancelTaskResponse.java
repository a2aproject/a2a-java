package org.a2aproject.sdk.compat03.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A response to a cancel task request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CancelTaskResponse extends JSONRPCResponse<Task> {

    @JsonCreator
    public CancelTaskResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                              @JsonProperty("result") Task result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    public CancelTaskResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }


    public CancelTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
