package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A JSON-RPC response containing task information.
 * This response provides detailed information about a task including its current status,
 * progress, metadata, and results (if completed). It is returned in response to a
 * GetTaskRequest and allows clients to monitor the state of long-running operations.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetTaskResponse extends JSONRPCResponse<Task> {

    /**
     * Creates a new GetTaskResponse with full parameter specification.
     * This constructor is primarily used by Jackson for JSON deserialization.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the response identifier matching the original request
     * @param result the task information if the request was successful
     * @param error the error information if the request failed
     */
    @JsonCreator
    public GetTaskResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                           @JsonProperty("result") Task result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, Task.class);
    }

    /**
     * Creates a successful GetTaskResponse with task information.
     * The JSON-RPC version defaults to "2.0" and no error is set.
     * 
     * @param id the response identifier matching the original request
     * @param result the task information including status, progress, and results
     */
    public GetTaskResponse(Object id, Task result) {
        this(null, id, result, null);
    }

    /**
     * Creates an error GetTaskResponse with error information.
     * The JSON-RPC version defaults to "2.0" and no result is set.
     * 
     * @param id the response identifier matching the original request
     * @param error the error information explaining why the request failed
     */
    public GetTaskResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }
    
}
