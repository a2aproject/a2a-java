package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON-RPC response to a cancel task request.
 * This response contains either the cancelled task information or an error if the cancellation failed.
 * A successful response will include the task with its state updated to "canceled".
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CancelTaskResponse extends JSONRPCResponse<Task> {

    /**
     * Creates a new CancelTaskResponse with full parameter specification.
     * This constructor is primarily used by Jackson for JSON deserialization.
     * 
     * @param jsonrpc the JSON-RPC protocol version (typically "2.0")
     * @param id the request identifier that matches the original request
     * @param result the cancelled task object (null if there was an error)
     * @param error the error information (null if the operation was successful)
     */
    @JsonCreator
    public CancelTaskResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                              @JsonProperty("result") Task result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    /**
     * Creates a new CancelTaskResponse for an error case.
     * The JSON-RPC version defaults to "2.0" and the result is set to null.
     * 
     * @param id the request identifier that matches the original request
     * @param error the error information describing why the cancellation failed
     */
    public CancelTaskResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    /**
     * Creates a new CancelTaskResponse for a successful case.
     * The JSON-RPC version defaults to "2.0" and the error is set to null.
     * 
     * @param id the request identifier that matches the original request
     * @param result the cancelled task object with updated state
     */
    public CancelTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }
}
